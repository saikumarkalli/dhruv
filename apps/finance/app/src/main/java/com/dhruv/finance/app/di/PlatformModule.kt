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
    }
