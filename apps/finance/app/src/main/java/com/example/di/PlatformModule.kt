package com.example.di

import com.dhruv.core.flags.FeatureFlag
import com.dhruv.core.flags.FeatureFlagResolver
import com.dhruv.core.flags.HardcodedFeatureFlagResolver
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.CrashlyticsReporter
import com.dhruv.core.observability.FirebasePerformanceTracer
import com.dhruv.core.observability.PerformanceTracer
import com.dhruv.finance.data.GeminiRepository
import com.example.BuildConfig
import org.koin.dsl.module

/**
 * App-level platform singletons shared by every feature module.
 *
 * Mirrors platform/feature-flags/dhruv-finance.json field-for-field (enabled + minVersion +
 * requiresConsent). At runtime the resolver precedence is remote -> cached -> these defaults
 * (PLATFORM.md §10); the hardcoded resolver is the always-available floor until
 * FirebaseFeatureFlagResolver is layered on. minVersion is gated against BuildConfig.VERSION_NAME,
 * so `assistant` (enabled, minVersion 1.2.0) stays hidden until the app ships >= 1.2.0.
 */
val financeFeatureDefaults: Map<String, FeatureFlag> = mapOf(
    "calculator" to FeatureFlag(enabled = true),
    "loans" to FeatureFlag(enabled = true),
    "investments" to FeatureFlag(enabled = true),
    "tax" to FeatureFlag(enabled = true),
    "everyday" to FeatureFlag(enabled = true),
    "currency" to FeatureFlag(enabled = true),
    "unit" to FeatureFlag(enabled = true),
    "date" to FeatureFlag(enabled = false),
    "time" to FeatureFlag(enabled = false),
    "assistant" to FeatureFlag(enabled = true, minVersion = "1.2.0", requiresConsent = true),
)

val platformModule = module {
    single<CrashReporter> { CrashlyticsReporter() }
    single<PerformanceTracer> { FirebasePerformanceTracer() }
    single<FeatureFlagResolver> {
        HardcodedFeatureFlagResolver(financeFeatureDefaults, BuildConfig.VERSION_NAME)
    }

    // Online AI. Key is supplied from the app BuildConfig (secrets plugin) so the repository,
    // which lives in :apps:finance:data, never reads app BuildConfig directly. See ADR in DECISIONS.md.
    single { GeminiRepository(BuildConfig.GEMINI_API_KEY) }
}
