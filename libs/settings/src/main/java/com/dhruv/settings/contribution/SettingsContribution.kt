package com.dhruv.settings.contribution

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * What a module publishes to appear in Settings' modules tier (contracts/settings-contribution.md
 * §1). Data only — the shell renders it, the module never draws its own Settings UI. [moduleKey]
 * MUST be a key that exists in the app's feature-flag file; the shell drops any contribution whose
 * key doesn't resolve to enabled (contract rule 1, `SET-ARCH-005`).
 *
 * [consentGranted] and [consentRequiredMessage] back FR-035/`SET-UI-008`: the shell already knows
 * *whether* a module needs consent from [com.dhruv.core.flags.FeatureFlagResolver.requiresConsent]
 * (moduleKey-keyed, no new metadata for that half — research R8) but has no generic way to know
 * *whether that consent is currently granted*, since different modules' consents live in different
 * places (assistant's in `SettingsRepository`, tracker's in `ConsentRepository`). A module that
 * requires consent states its own current grant state here; one that doesn't leaves the defaults,
 * which read as "always granted" and are therefore inert for `ModuleSettingsScreen`.
 */
data class SettingsContribution(
    val moduleKey: String,
    @StringRes val title: Int,
    @StringRes val summary: Int,
    val order: Int,
    val groups: List<SettingsGroup>,
    val consentGranted: Flow<Boolean> = flowOf(true),
    @StringRes val consentRequiredMessage: Int? = null,
    /**
     * Whether the user may turn this module off entirely (FR-032). **Defaults to `false`** — a
     * module is only optional when it says so, because FR-033 forbids making a primary navigation
     * destination user-hideable and defaulting the other way would offer that control to every
     * contribution automatically.
     *
     * `calculator` is the worked example: its `moduleKey` does not collide with any `TabKey` name,
     * yet it *is* the Calc tab's content — so a name-collision check alone (`PrimaryDestinationTest`)
     * cannot decide this, and the contribution has to declare it.
     */
    val optional: Boolean = false,
)
