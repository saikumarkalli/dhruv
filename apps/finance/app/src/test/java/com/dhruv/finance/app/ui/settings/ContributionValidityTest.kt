package com.dhruv.finance.app.ui.settings

import com.dhruv.settings.contribution.SettingsRegistry
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `SET-ARCH-005`: every registered contribution's `moduleKey` exists in
 * `platform/feature-flags/dhruv-finance.json`. The known-keys set below mirrors that file by hand
 * (same convention `FeatureFlagAssetLoaderTest.FINANCE_FLAGS_JSON` already uses) rather than
 * reading it from disk, so this stays a fast, hermetic JVM test.
 *
 * Lives at the app level, not in `:libs:settings` (where `SettingsRegistryTest` covers the same
 * check with fake data — that test proves the *mechanism*; this one proves the *real contributions*
 * this phase actually ships pass it). `:libs:settings` has no access to real feature-module
 * contributions or the flags asset, so a fully faithful version of this check can only run here.
 */
class ContributionValidityTest {
    // Mirrors platform/feature-flags/dhruv-finance.json's key set — kept in sync manually.
    private val knownFlagKeys =
        setOf(
            "calculator", "loans", "investments", "tax", "everyday",
            "currency", "unit", "date", "time", "assistant", "networth",
        )

    @Test
    fun `every registered contribution's moduleKey is a real flag key`() {
        val contributions = realSettingsContributions()

        val unknown = SettingsRegistry().unknownModuleKeys(contributions, knownFlagKeys)
        assertTrue("unknown moduleKey(s), not in the flags file: $unknown", unknown.isEmpty())
    }
}
