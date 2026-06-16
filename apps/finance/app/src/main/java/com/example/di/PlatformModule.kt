package com.example.di

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
 * Mirrors the hardcoded defaults in platform/feature-flags/dhruv-finance.json. At runtime the
 * resolver precedence is remote -> cached -> these defaults (PLATFORM.md §10); the hardcoded
 * resolver is the always-available floor until FirebaseFeatureFlagResolver is layered on.
 */
val financeFeatureDefaults: Map<String, Boolean> = mapOf(
    "calculator" to true,
    "loans" to true,
    "investments" to true,
    "tax" to true,
    "everyday" to true,
    "currency" to true,
    "unit" to true,
    "date" to false,
    "time" to false,
    "assistant" to true,
)

val platformModule = module {
    single<CrashReporter> { CrashlyticsReporter() }
    single<PerformanceTracer> { FirebasePerformanceTracer() }
    single<FeatureFlagResolver> { HardcodedFeatureFlagResolver(financeFeatureDefaults) }

    // Online AI. Key is supplied from the app BuildConfig (secrets plugin) so the repository,
    // which lives in :apps:finance:data, never reads app BuildConfig directly. See ADR in DECISIONS.md.
    single { GeminiRepository(BuildConfig.GEMINI_API_KEY) }
}
