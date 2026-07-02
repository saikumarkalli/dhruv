package com.dhruv.finance.app.di

import android.content.Context
import com.dhruv.core.flags.FeatureFlag
import com.dhruv.core.observability.CrashReporter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.io.IOException

@JsonClass(generateAdapter = true)
data class FeatureFlagDto(
    val enabled: Boolean,
    val minVersion: String = "0.0.0",
    val requiresConsent: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class FeatureFlagsFileDto(
    val app: String,
    val features: Map<String, FeatureFlagDto>,
)

// Last-resort fallback if the bundled asset is ever missing or fails to parse. Calculator-only
// (minVersion default "0.0.0") so the app degrades to its primary feature rather than going blank.
private val safetyDefaults: Map<String, FeatureFlag> =
    mapOf(
        "calculator" to FeatureFlag(enabled = true),
    )

// Broad catch is intentional: any JSON/parse failure must fall back to the calculator-only
// safety map (and report it) so a malformed flags asset can never blank the app.
@Suppress("TooGenericExceptionCaught")
internal fun parseFeatureFlags(
    json: String,
    crashReporter: CrashReporter,
): Map<String, FeatureFlag> =
    try {
        val adapter = Moshi.Builder().build().adapter(FeatureFlagsFileDto::class.java)
        val parsed = adapter.fromJson(json)
        parsed?.features?.mapValues { (_, dto) ->
            FeatureFlag(dto.enabled, dto.minVersion, dto.requiresConsent)
        } ?: safetyDefaults
    } catch (e: Exception) {
        crashReporter.recordException(e)
        safetyDefaults
    }

/**
 * Loads feature flag defaults from the platform/feature-flags/dhruv-finance.json asset (packaged
 * via the app module's assets.srcDirs), making that file the single source of truth instead of a
 * hand-duplicated Kotlin literal.
 */
fun loadFinanceFeatureFlags(
    context: Context,
    crashReporter: CrashReporter,
): Map<String, FeatureFlag> =
    try {
        val json =
            context.assets
                .open("dhruv-finance.json")
                .bufferedReader()
                .use { it.readText() }
        parseFeatureFlags(json, crashReporter)
    } catch (e: IOException) {
        crashReporter.recordException(e)
        safetyDefaults
    }
