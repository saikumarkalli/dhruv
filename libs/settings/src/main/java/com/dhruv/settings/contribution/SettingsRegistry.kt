package com.dhruv.settings.contribution

import com.dhruv.core.flags.FeatureFlagResolver

/**
 * Resolves, filters and orders every registered [SettingsContribution] (contract §4). Pure: takes
 * an already-Koin-resolved list rather than reaching into a container itself, so it is unit-testable
 * without Koin or Robolectric. The caller resolves `getAll<SettingsContribution>()` once per Settings
 * open (contract rule 13) and passes the result here.
 */
class SettingsRegistry {
    /**
     * @param titleOf resolves a contribution's string-resource [SettingsContribution.title] to
     * comparable text for the `order` tie-break (contract §4 rule 3) — a raw resource id is a
     * compile-time constant with no relation to the string's alphabetical text, so the caller
     * supplies the real resolution (`Context.getString` / `stringResource`); tests supply a fake.
     */
    fun resolve(
        contributions: List<SettingsContribution>,
        resolver: FeatureFlagResolver,
        titleOf: (Int) -> String,
    ): List<SettingsContribution> =
        contributions
            .filter { resolver.isEnabled(it.moduleKey) }
            .sortedWith(compareBy({ it.order }, { titleOf(it.title) }))

    /** `SET-ARCH-005`: every registered contribution's `moduleKey` must be a real flag key. */
    fun unknownModuleKeys(
        contributions: List<SettingsContribution>,
        knownFlagKeys: Set<String>,
    ): List<String> = contributions.map { it.moduleKey }.filterNot { it in knownFlagKeys }
}
