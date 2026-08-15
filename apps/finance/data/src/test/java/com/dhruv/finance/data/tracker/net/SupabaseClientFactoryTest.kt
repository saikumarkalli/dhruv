package com.dhruv.finance.data.tracker.net

import com.dhruv.finance.data.FakeSessionStore
import com.dhruv.finance.data.tracker.auth.TrackerRpcApi
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * DAT-BR-005: CertificatePinner config is inspected -> pins match Google Trust Services GTS Root
 * R1/R4 (CA level), not a leaf certificate (ADR-0014 §6, ADR-0029 decision 6). No network involved
 * — this reads the configured [okhttp3.CertificatePinner] directly.
 *
 * Pin values corrected live, 2026-08-15 — a real device hitting the real Supabase host through
 * this exact pinner threw `SSLPeerUnverifiedException` against the originally-pinned ISRG Root
 * X1/X2 (Let's Encrypt); the actual chain roots to Google Trust Services' GTS Root R4. Verified
 * independently against Google's own published trust store
 * (https://pki.goog/repo/certs/gtsr1.der, gtsr4.der) by computing `sha256(SubjectPublicKeyInfo)`
 * with openssl and confirming an exact match — not merely copied from the live failure's own
 * peer-chain dump.
 */
class SupabaseClientFactoryTest {
    private val factory =
        SupabaseClientFactory(
            supabaseUrl = "https://dsfnrtckgpnvyvscevxn.supabase.co",
            anonKey = "anon-key",
            sessionStore = FakeSessionStore(),
            hasSyncConsent = { true },
        )

    // DAT-BR-005
    @Test
    fun `certificate pinner pins the Supabase host to GTS Root R1 and R4, CA level`() {
        val pins = factory.certificatePinner.pins

        assertEquals(2, pins.size)
        assertTrue(pins.all { it.pattern == "dsfnrtckgpnvyvscevxn.supabase.co" })
        val pinStrings = pins.map { it.toString() }
        assertTrue(pinStrings.contains("sha256/hxqRlPTu1bMS/0DITB1SSu0vd4u/8l8TjPgfaAp63Gc="))
        assertTrue(pinStrings.contains("sha256/mEflZT5enoR1FuXLgYYGqnVEoZvmf9c2bVBpiOjYQ0c="))
    }

    // DAT-BR-005 — host is parsed from SUPABASE_URL, never a hardcoded domain
    @Test
    fun `pin pattern is derived from the configured supabaseUrl hostname, not hardcoded`() {
        val otherHostFactory =
            SupabaseClientFactory(
                supabaseUrl = "https://some-other-project.supabase.co",
                anonKey = "anon-key",
                sessionStore = FakeSessionStore(),
                hasSyncConsent = { true },
            )

        val pattern =
            otherHostFactory.certificatePinner.pins
                .first()
                .pattern

        assertEquals("some-other-project.supabase.co", pattern)
    }

    // DAT-BR-001 — structural: ConsentInterceptor must only ever be attached to dataClient
    @Test
    fun `only dataClient carries a ConsentInterceptor, authClient never does`() {
        assertTrue(factory.authClient.interceptors.none { it is ConsentInterceptor })
        assertTrue(factory.dataClient.interceptors.any { it is ConsentInterceptor })
    }

    // DAT-BR-002/003 — both clients carry AuthInterceptor (every tracker request is authenticated)
    @Test
    fun `both clients carry AuthInterceptor`() {
        assertTrue(factory.authClient.interceptors.any { it is AuthInterceptor })
        assertTrue(factory.dataClient.interceptors.any { it is AuthInterceptor })
    }

    // Structural regression lock for the Critical bug found in Task 4 review: erasureRetrofit must
    // share authClient (no ConsentInterceptor), never dataClient.
    @Test
    fun `erasureRetrofit carries AuthInterceptor but never ConsentInterceptor`() {
        val erasureClient = factory.erasureRetrofit.callFactory() as OkHttpClient

        assertTrue(erasureClient.interceptors.any { it is AuthInterceptor })
        assertTrue(erasureClient.interceptors.none { it is ConsentInterceptor })
    }

    // Fix 2 (final whole-branch review, Important) — a fresh clone building against
    // .env.example's own placeholder SUPABASE_URL default (MY_SUPABASE_URL, no http(s) scheme)
    // must not crash the whole app at launch: MainActivity resolves ConsentRepository/
    // TrackerAccountRepository (which transitively construct SupabaseClientFactory via Koin)
    // eagerly inside setContent, before anything renders and outside every FeatureHost.
    // Construction must succeed regardless; only a genuine network call against the resulting
    // client should ever fail, and it should fail normally (a caught exception / TrackerError),
    // not as a process crash at construction time.
    @Test
    fun `an invalid placeholder supabaseUrl does not crash construction`() {
        val factory =
            SupabaseClientFactory(
                supabaseUrl = "MY_SUPABASE_URL",
                anonKey = "MY_SUPABASE_ANON_KEY",
                sessionStore = FakeSessionStore(),
                hasSyncConsent = { true },
            )

        // No host could be parsed from the placeholder, so nothing is pinned — pinning an invalid
        // host would be meaningless, and CertificatePinner enforces nothing for hosts with no
        // registered pins, so this doesn't weaken pinning for a genuinely configured deployment.
        assertTrue(factory.certificatePinner.pins.isEmpty())
        assertNotNull(factory.authApi)
        assertNotNull(factory.dataRetrofit)
        assertNotNull(factory.erasureRetrofit)
    }
}

/**
 * Behavioral regression lock for the Critical bug found in Task 4 review (ONB-BR-008/009):
 * [SupabaseClientFactory.erasureRetrofit] must actually dispatch a request to the wire even when
 * "Sync my financial records" consent is declined, while [SupabaseClientFactory.dataRetrofit] must
 * still be blocked before dispatch under the exact same declined-consent condition. A separate
 * top-level class (not nested in [SupabaseClientFactoryTest]) so JUnit4 actually discovers and runs
 * it — a nested class without `@RunWith(Enclosed::class)` on the outer class is silently skipped.
 * Uses a real [MockWebServer] (plain HTTP, so [okhttp3.CertificatePinner]'s TLS-only check never
 * engages) so this is proven end-to-end through the real client chains, not just by inspecting
 * interceptor lists (which the structural test above already does).
 */
class SupabaseClientFactoryErasureBypassesConsentTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun declinedConsentFactory() =
        SupabaseClientFactory(
            supabaseUrl = server.url("/").toString(),
            anonKey = "anon-key",
            sessionStore = FakeSessionStore(),
            hasSyncConsent = { false },
        )

    // ONB-BR-008/009 — the bug: dataRetrofit would permanently block erasure for anyone who
    // declined or withdrew sync consent, which is exactly who is most likely to press "Delete my
    // data"/"Delete my account".
    @Test
    fun `erasureRetrofit dispatches a real request despite declined consent`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(200).setBody("null"))
            val factory = declinedConsentFactory()

            factory.erasureRetrofit.create(TrackerRpcApi::class.java).deleteMyData()

            assertEquals(1, server.requestCount)
        }

    // The converse, proven against the same declined-consent factory: dataRetrofit is still gated
    // — this fix must not have accidentally removed consent enforcement from the path every other
    // (Phase 2+) tracker data call will use.
    @Test
    fun `dataRetrofit is still blocked before dispatch under the same declined consent`() =
        runTest {
            val factory = declinedConsentFactory()

            try {
                factory.dataRetrofit.create(TrackerRpcApi::class.java).deleteMyData()
                fail("expected dataRetrofit to be blocked by ConsentInterceptor when consent is declined")
            } catch (expected: ConsentRequiredException) {
                // blocked before dispatch, as expected
            }

            assertEquals(0, server.requestCount)
        }
}
