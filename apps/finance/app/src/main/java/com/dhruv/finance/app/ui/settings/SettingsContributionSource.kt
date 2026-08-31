package com.dhruv.finance.app.ui.settings

import com.dhruv.settings.contribution.SettingsContribution
import org.koin.core.Koin

/**
 * Resolves every registered [SettingsContribution] from the DI container by type (research R1) —
 * the shell never imports a specific module's contribution class, only this type. Wrapping [Koin]
 * here (rather than calling `getAll()` inline in a composable) keeps the container API out of
 * screen code and matches contract §4 rule 13: [resolve] is called once per Settings open.
 */
class SettingsContributionSource(
    private val koin: Koin,
) {
    fun resolve(): List<SettingsContribution> = koin.getAll()
}
