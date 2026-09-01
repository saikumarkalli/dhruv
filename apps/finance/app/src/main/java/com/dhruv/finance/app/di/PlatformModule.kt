package com.dhruv.finance.app.di

import com.dhruv.core.flags.FeatureFlagResolver
import com.dhruv.core.flags.HardcodedFeatureFlagResolver
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.CrashlyticsReporter
import com.dhruv.core.observability.FirebasePerformanceTracer
import com.dhruv.core.observability.PerformanceTracer
import com.dhruv.finance.app.BuildConfig
import com.dhruv.finance.data.AppDatabase
import com.dhruv.finance.data.GeminiRepository
import com.dhruv.finance.data.HistoryRepository
import com.dhruv.finance.data.api.CurrencyApiClient
import com.dhruv.finance.data.tracker.auth.AuthRepository
import com.dhruv.finance.data.tracker.auth.AuthRepositoryImpl
import com.dhruv.finance.data.tracker.auth.ConsentRepository
import com.dhruv.finance.data.tracker.auth.ConsentRepositoryImpl
import com.dhruv.finance.data.tracker.auth.SessionStore
import com.dhruv.finance.data.tracker.auth.SessionStoreImpl
import com.dhruv.finance.data.tracker.auth.TrackerAccountRepository
import com.dhruv.finance.data.tracker.auth.TrackerAccountRepositoryImpl
import com.dhruv.finance.data.tracker.net.SupabaseClientFactory
import com.dhruv.finance.data.tracker.repo.HoldingRepository
import com.dhruv.finance.data.tracker.repo.HoldingRepositoryImpl
import com.dhruv.finance.data.tracker.repo.NetWorthRepository
import com.dhruv.finance.data.tracker.repo.NetWorthRepositoryImpl
import com.dhruv.finance.onboarding.GoogleSignInConfig
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * App-level platform singletons shared by every feature module.
 *
 * Feature flag defaults are loaded at runtime from platform/feature-flags/dhruv-finance.json
 * (see [loadFinanceFeatureFlags]) instead of a hand-duplicated literal, so that file is the
 * single source of truth. At runtime the resolver precedence is remote -> cached -> these
 * defaults (PLATFORM.md §10); the hardcoded resolver is the always-available floor until
 * FirebaseFeatureFlagResolver is layered on. minVersion is gated against BuildConfig.VERSION_NAME,
 * so `assistant` (enabled, minVersion 1.2.0) stays hidden until the app ships >= 1.2.0.
 */
val platformModule =
    module {
        single<CrashReporter> { CrashlyticsReporter() }
        single<PerformanceTracer> { FirebasePerformanceTracer() }
        single<FeatureFlagResolver> {
            HardcodedFeatureFlagResolver(loadFinanceFeatureFlags(androidContext(), get()), BuildConfig.VERSION_NAME)
        }

        // Online AI. Key is supplied from the app BuildConfig (secrets plugin) so the repository,
        // which lives in :apps:finance:data, never reads app BuildConfig directly. See ADR in DECISIONS.md.
        single { GeminiRepository(BuildConfig.GEMINI_API_KEY) }

        // Room DB, currency API client, and history retention all need BuildConfig values, so (like
        // GeminiRepository above) they're constructed here in the app module and injected into :data.
        single {
            AppDatabase.getDatabase(androidContext(), BuildConfig.APP_DATABASE_NAME)
        }
        single {
            CurrencyApiClient(
                primaryBaseUrl = BuildConfig.CURRENCY_API_BASE_URL,
                fallbackBaseUrl = BuildConfig.CURRENCY_API_FALLBACK_BASE_URL,
                timeoutSeconds = BuildConfig.CURRENCY_API_TIMEOUT_SECONDS,
                userAgent = BuildConfig.CURRENCY_API_USER_AGENT,
            )
        }
        single {
            HistoryRepository(get(), BuildConfig.HISTORY_RECYCLE_BIN_RETENTION_MILLIS)
        }

        // Tracker backend (ADR-0029). SUPABASE_URL/SUPABASE_ANON_KEY are BuildConfig-sourced
        // (secrets plugin, .env) for the same reason as CurrencyApiClient above — :data can't
        // read app BuildConfig directly. SessionStore/ConsentRepository are constructed here
        // (context.applicationContext, same shape as SettingsRepositoryImpl) and injected into
        // SupabaseClientFactory, which gates dataClient behind the sync-financial-records switch.
        single<SessionStore> { SessionStoreImpl(androidContext(), get()) }
        single<ConsentRepository> { ConsentRepositoryImpl(androidContext(), get()) }
        single {
            SupabaseClientFactory(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                anonKey = BuildConfig.SUPABASE_ANON_KEY,
                sessionStore = get(),
                hasSyncConsent = { get<ConsentRepository>().state.value.syncFinancialRecords },
            )
        }
        // Sign-in is pre-consent (ONB-BR-001) — AuthRepository composes SupabaseClientFactory's
        // authApi with SessionStore so OnboardingViewModel never touches either directly.
        single<AuthRepository> { AuthRepositoryImpl(get<SupabaseClientFactory>().authApi, get()) }

        // Settings > Privacy erasure (ONB-BR-008/009). TrackerAccountRepositoryImpl's
        // SupabaseClientFactory-taking constructor builds TrackerRpcApi off erasureRetrofit —
        // auth-gated but deliberately NOT consent-gated, so erasure stays reachable even when the
        // "Sync my financial records" switch is off (ADR-0014 §7) — without needing
        // retrofit2.Retrofit itself on this module's compile classpath (it's `implementation`-
        // scoped inside :data).
        single<TrackerAccountRepository> {
            TrackerAccountRepositoryImpl(
                supabaseClientFactory = get(),
                sessionStore = get(),
                consentRepository = get(),
            )
        }

        // Net worth tracker (Phase 2, C1-C7). Both repositories build their Retrofit API
        // interfaces off SupabaseClientFactory.dataRetrofit (consent-gated), same convenience-
        // constructor pattern as TrackerAccountRepositoryImpl above.
        single<HoldingRepository> { HoldingRepositoryImpl(get<SupabaseClientFactory>()) }
        single<NetWorthRepository> { NetWorthRepositoryImpl(get<SupabaseClientFactory>()) }

        // A2 sign-in's Credential Manager call needs the Web client id; sourced from app
        // BuildConfig here (secrets plugin) for the same reason as GeminiRepository/SupabaseClientFactory
        // above — :apps:finance:feature:onboarding can't read app BuildConfig directly.
        single { GoogleSignInConfig(BuildConfig.GOOGLE_WEB_CLIENT_ID) }
    }
