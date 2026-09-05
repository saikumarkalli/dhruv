package com.dhruv.finance.app.ui.settings

import com.dhruv.finance.assistant.settings.assistantSettingsContribution
import com.dhruv.finance.calculator.settings.calculatorSettingsContribution
import com.dhruv.finance.currency.settings.currencySettingsContribution
import com.dhruv.finance.data.HistoryRepository
import com.dhruv.finance.networth.settings.netWorthSettingsContribution
import com.dhruv.finance.unit.settings.unitSettingsContribution
import com.dhruv.settings.contribution.SettingsContribution

/**
 * The **real** `SettingsContribution`s this app ships, built from each feature module's own factory
 * — not hand-written stand-ins. One list, so a test that claims to check "the real contributions"
 * cannot silently drift from the ones actually registered.
 *
 * Consumed by `ContributionValidityTest` (`SET-ARCH-005`), `AlertControlCoverageTest`
 * (`SET-BR-006`, FR-031) and `PrimaryDestinationTest` (`SET-UI-010`). Before this existed, each
 * fabricated its own contributions inline — so `AlertControlCoverageTest`'s "the real registered
 * channel registry" case passed against a *hardcoded* `alert_daily_rates` toggle and would have
 * kept passing if the currency module dropped the real one.
 *
 * **Residual limitation, stated rather than hidden:** this list is still hand-maintained. Adding a
 * feature module without adding it here means these tests silently stop covering it. The
 * non-drifting source is Koin's own `getAll<SettingsContribution>()`, whose mechanism
 * `ContributionResolutionProbeTest` covers directly; wiring the full DI graph into every one of
 * these JVM tests costs more than it buys today, at four modules.
 */
fun realSettingsContributions(
    settingsRepository: FakeSettingsRepository = FakeSettingsRepository(),
    historyRepository: HistoryRepository = HistoryRepository(historyDao = FakeHistoryDao()),
): List<SettingsContribution> =
    listOf(
        calculatorSettingsContribution(settingsRepository, historyRepository),
        currencySettingsContribution(settingsRepository),
        unitSettingsContribution(),
        assistantSettingsContribution(settingsRepository),
        netWorthSettingsContribution(),
    )
