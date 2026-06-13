package com.dhruv.core.flags

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.tasks.await

interface FeatureFlagResolver {
    fun isEnabled(key: String): Boolean
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

/** Deterministic resolver for tests and pre-Firebase environments. */
class HardcodedFeatureFlagResolver(
    private val defaults: Map<String, Boolean>
) : FeatureFlagResolver {
    override fun isEnabled(key: String): Boolean = defaults[key] ?: false
}
