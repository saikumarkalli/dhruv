package com.dhruv.core.flags

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.tasks.await

interface FeatureFlagResolver {
    fun isEnabled(key: String): Boolean

    /**
     * True when the feature must show a DPDP consent gate before any data leaves the device
     * (PLATFORM.md §8). Defaults to false; resolvers backed by a [FeatureFlag] model override it.
     */
    fun requiresConsent(key: String): Boolean = false
}

/**
 * Firebase Remote Config resolver with in-memory cache and hardcoded fallback.
 * Call fetch() once at app start (or on settings change) to refresh values.
 *
 * Precedence: Remote Config → cached → defaults map.
 */
class FirebaseFeatureFlagResolver(
    private val defaults: Map<String, Boolean> = emptyMap(),
    minimumFetchIntervalSeconds: Long = 3600L,
) : FeatureFlagResolver {

    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance().also {
        it.setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(minimumFetchIntervalSeconds)
                .build()
        )
        it.setDefaultsAsync(defaults.mapValues { (_, v) -> v })
    }

    suspend fun fetch() {
        runCatching { remoteConfig.fetchAndActivate().await() }
    }

    override fun isEnabled(key: String): Boolean =
        remoteConfig.getBoolean(key)
}

/**
 * Deterministic resolver for tests and pre-Firebase environments.
 *
 * Honors each flag's [FeatureFlag.minVersion] against [currentVersion]: a flag may be declared
 * [FeatureFlag.enabled] yet stay disabled until the running app reaches its minVersion (e.g.
 * `assistant` is `enabled = true` but gated to 1.2.0). Unknown keys are disabled and consent-free.
 */
class HardcodedFeatureFlagResolver(
    private val flags: Map<String, FeatureFlag>,
    currentVersion: String = "0.0.0",
) : FeatureFlagResolver {

    private val appVersion = SemVer.parse(currentVersion)

    override fun isEnabled(key: String): Boolean {
        val flag = flags[key] ?: return false
        return flag.enabled && appVersion >= SemVer.parse(flag.minVersion)
    }

    override fun requiresConsent(key: String): Boolean = flags[key]?.requiresConsent ?: false
}
