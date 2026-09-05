package com.dhruv.finance.data.tracker.net

import com.dhruv.finance.data.tracker.auth.GoTrueApi
import com.dhruv.finance.data.tracker.auth.SessionStore
import okhttp3.CertificatePinner
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

// Corrected live, 2026-08-15: ADR-0014 §6 specified ISRG Root X1/X2 (Let's Encrypt) on the
// (incorrect) assumption that's what Supabase's TLS chain roots to. A real device hitting the real
// `*.supabase.co` domain through this exact CertificatePinner threw
// `SSLPeerUnverifiedException: Certificate pinning failure!` on the very first live call this
// session (every earlier "live" verification this session went through the Management API's
// `api.supabase.co` or the Postgres wire protocol — neither touches this pinned OkHttp client, so
// the wrong pins went undetected until Google sign-in's REST call was the first real exercise of
// it). The exception's own peer-chain dump showed the actual root: Google Trust Services' GTS Root
// R4 (`CN=GTS Root R4,O=Google Trust Services LLC,C=US`). Both pins below are independently
// verified against Google's own published trust store (`https://pki.goog/repo/certs/gtsr{1,4}.der`,
// SPKI SHA-256 computed via openssl), not just copied from the live capture — R4 (ECC) is the root
// actually observed, R1 (RSA) is pinned alongside it for the same redundancy-across-key-types
// reason the original ISRG X1(RSA)/X2(ECC) pair existed, in case Supabase's CDN ever serves an
// RSA-chain leaf instead.
private const val GTS_ROOT_R1_PIN = "sha256/hxqRlPTu1bMS/0DITB1SSu0vd4u/8l8TjPgfaAp63Gc="
private const val GTS_ROOT_R4_PIN = "sha256/mEflZT5enoR1FuXLgYYGqnVEoZvmf9c2bVBpiOjYQ0c="
private const val TIMEOUT_SECONDS = 15L

/** Syntactically valid but unroutable — substituted for [SupabaseClientFactory.baseUrl] only when
 * the configured `SUPABASE_URL` fails to parse (Fix 2, final whole-branch review). Keeps every
 * Retrofit client constructible (`Retrofit.Builder.baseUrl` requires an absolute URL just like
 * `toHttpUrlOrNull` does) so a broken/placeholder config fails at the first real network call —
 * a normal, catchable failure — instead of crashing the whole app at construction time. */
private const val UNCONFIGURED_FALLBACK_BASE_URL = "https://supabase-url-not-configured.invalid"

/**
 * Builds the Supabase Retrofit clients (ADR-0029): an unauthenticated-consent [authApi] (GoTrue),
 * a consent-gated [dataRetrofit] (PostgREST), and an auth-only-gated [erasureRetrofit] (PostgREST,
 * for the DPDP erasure RPCs — see its own doc comment). Mirrors `CurrencyApiClient`'s shape — one
 * shared OkHttpClient config, `Retrofit.Builder` per base path — but needs two distinct OkHttpClient
 * *chains*, not one shared client, because `authClient` and `dataClient` carry different interceptor
 * sets:
 * - `authClient`: [AuthInterceptor] only — sign-in itself is pre-consent (ONB-BR-001), and this is
 *   also the chain [erasureRetrofit] reuses, since erasure must survive a declined/withdrawn
 *   consent state by design.
 * - `dataClient`: [ConsentInterceptor] + [AuthInterceptor] — every *other* PostgREST call must go
 *   through this client so consent is enforced by construction, not convention (ADR-0029).
 *
 * Certificate pinning is CA-level (Google Trust Services GTS Root R1 + R4, DAT-BR-005 — corrected
 * live 2026-08-15, see the pin constants' doc comment for why it isn't ISRG Root X1/X2 as
 * ADR-0014 §6 originally specified) on both underlying clients, hostname parsed from [supabaseUrl]
 * rather than hardcoded, so pinning always tracks configuration.
 */
class SupabaseClientFactory(
    supabaseUrl: String,
    anonKey: String,
    sessionStore: SessionStore,
    hasSyncConsent: () -> Boolean,
) {
    // Fix 2 (final whole-branch review, Important): an invalid/placeholder SUPABASE_URL (e.g.
    // .env.example's own default, "MY_SUPABASE_URL" — no http(s) scheme) must not crash the whole
    // app at launch. MainActivity resolves ConsentRepository/TrackerAccountRepository (which
    // transitively construct this factory via Koin) eagerly inside setContent, before anything
    // renders and outside every FeatureHost, so a `.toHttpUrl()`/`Retrofit.baseUrl()` throw here
    // previously took the whole app — calculators included — down on any build without a real
    // `.env`. `host` is null, and `baseUrl` falls back to a syntactically valid but unroutable
    // URL, exactly when the configured value doesn't parse; SessionStore/ConsentRepository don't
    // depend on this factory at all, and every Retrofit client below still constructs normally —
    // a genuinely broken config only ever surfaces as an ordinary failed network call.
    private val requestedBaseUrl = supabaseUrl.trimEnd('/')
    private val host = "$requestedBaseUrl/".toHttpUrlOrNull()?.host
    private val baseUrl = if (host != null) requestedBaseUrl else UNCONFIGURED_FALLBACK_BASE_URL

    val certificatePinner: CertificatePinner =
        CertificatePinner
            .Builder()
            .apply { if (host != null) add(host, GTS_ROOT_R1_PIN, GTS_ROOT_R4_PIN) }
            .build()

    private fun baseClientBuilder(): OkHttpClient.Builder =
        OkHttpClient
            .Builder()
            .certificatePinner(certificatePinner)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)

    private fun moshiRetrofit(
        base: String,
        client: OkHttpClient,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

    /** Forward-referenced by [authInterceptor]'s refresh-call lambda; only invoked (never read
     * before) the first 401, by which point construction has fully completed. */
    lateinit var authApi: GoTrueApi
        private set

    private val authInterceptor: AuthInterceptor = AuthInterceptor(sessionStore, anonKey) { authApi }

    internal val authClient: OkHttpClient =
        baseClientBuilder()
            .addInterceptor(authInterceptor)
            .build()

    init {
        authApi = moshiRetrofit("$baseUrl/auth/v1/", authClient).create(GoTrueApi::class.java)
    }

    internal val dataClient: OkHttpClient =
        baseClientBuilder()
            .addInterceptor(ConsentInterceptor(hasSyncConsent))
            .addInterceptor(FinanceSchemaInterceptor())
            .addInterceptor(authInterceptor)
            .build()

    /** PostgREST Retrofit instance, consent- and auth-gated. Every endpoint built on this Retrofit
     * instance automatically sends PostgREST's `finance`-schema headers via [FinanceSchemaInterceptor]
     * (ADR-0033) — `holdings`/`valuations`/their views live in the `finance` Postgres schema, not
     * `public`, and omitting the header would silently 404 against the (empty) `public` schema
     * instead. First consumers: [com.dhruv.finance.data.tracker.repo.HoldingApi],
     * [com.dhruv.finance.data.tracker.repo.ValuationApi],
     * [com.dhruv.finance.data.tracker.repo.NetWorthApi] (Phase 2). */
    val dataRetrofit: Retrofit = moshiRetrofit("$baseUrl/rest/v1/", dataClient)

    /** PostgREST Retrofit instance for calls that must succeed regardless of consent state — today
     * only the erasure RPCs (delete_my_data/delete_my_account, ADR-0014 §7, ADR-0029). Auth-gated
     * (shares [authClient]'s OkHttpClient, so [AuthInterceptor] still attaches apikey/Bearer) but
     * deliberately NOT consent-gated: DPDP erasure must remain reachable even when — especially
     * when — the user has declined or withdrawn "Sync my financial records" (ONB-BR-002/ONB-BR-008/
     * ONB-BR-009). Do not build any other PostgREST call against this client; every other tracker
     * data path (Phase 2+) must go through the consent-gated [dataRetrofit] above.
     *
     * No schema header needed here (ADR-0033): [TrackerRpcApi]'s two functions stay in the
     * default `public` schema by design — they're the cross-app erasure orchestrators, not
     * `finance`-domain objects (see `supabase/schemas/public/30_functions/`). */
    val erasureRetrofit: Retrofit = moshiRetrofit("$baseUrl/rest/v1/", authClient)
}
