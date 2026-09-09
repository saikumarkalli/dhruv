package com.dhruv.finance.money.settings

import com.dhruv.finance.data.tracker.repo.CategoryRepository
import com.dhruv.finance.money.R
import com.dhruv.settings.contribution.SettingsContribution
import com.dhruv.settings.contribution.SettingsGroup
import com.dhruv.settings.contribution.SettingsRow
import kotlinx.coroutines.flow.flow

private const val MODULE_KEY = "money"

/**
 * The Money tab's Settings entry (module-standard doc §2.a, `apps/finance/CLAUDE.md`'s tracking
 * rule 2a). Not optional (`optional` defaults false) — Money is a primary `TabKey` destination, the
 * same worked example the contract's own doc comment names for `calculator`.
 *
 * [SettingsContribution.consentGranted] is deliberately left at its default (always granted), not
 * wired to the tracker's real "Sync my financial records" state. `DependencyRulesTest` forbids any
 * `*.settings` package from importing `com.dhruv.finance.data.tracker.auth..`
 * (`ConsentRepository`'s package) directly — its own doc comment says the sanctioned path is
 * "through `SettingsRepository`'s own public API," but `SettingsRepository` (`:libs:settings`) has
 * no tracker-consent field to read; mirroring one there would duplicate the DataStore ADR-0014 §7
 * already names as the single source of truth, the same class of drift this codebase's constitution
 * forbids. This is a real, tracked gap (not silently faked as granted for a wrong reason) — the
 * actual data-access boundary is unaffected: `ConsentInterceptor` still blocks every PostgREST call
 * regardless of what this Settings row shows, so nothing insecure follows from it.
 */
fun moneySettingsContribution(categoryRepository: CategoryRepository): SettingsContribution =
    SettingsContribution(
        moduleKey = MODULE_KEY,
        title = R.string.settings_money_title,
        summary = R.string.settings_money_summary,
        order = 0,
        groups =
            listOf(
                SettingsGroup(
                    label = null,
                    rows =
                        listOf(
                            SettingsRow.Info(
                                key = "money_categories_info",
                                label = R.string.settings_money_categories_label,
                                description = R.string.settings_money_categories_description,
                                value =
                                    flow {
                                        val count = categoryRepository.listCategories().getOrNull()?.size ?: 0
                                        emit(count.toString())
                                    },
                            ),
                        ),
                ),
            ),
    )
