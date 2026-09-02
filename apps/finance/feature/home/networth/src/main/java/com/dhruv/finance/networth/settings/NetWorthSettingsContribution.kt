package com.dhruv.finance.networth.settings

import com.dhruv.finance.data.tracker.model.LiabilityType
import com.dhruv.finance.data.tracker.model.Sector
import com.dhruv.finance.networth.R
import com.dhruv.settings.contribution.SettingsContribution
import com.dhruv.settings.contribution.SettingsGroup
import com.dhruv.settings.contribution.SettingsRow
import kotlinx.coroutines.flow.flowOf

private const val MODULE_KEY = "networth"

/**
 * The net worth tracker's Settings entry (001-net-worth-tracker Phase 9, T059) — 004-settings'
 * "every later phase ships its module's settings entry with the module" rule, previously unmet by
 * this module. Not `optional` (unlike `currency`/`unit`/`assistant`): net worth is Home's own tab
 * content, the same reasoning `calculator` already uses for Calc (FR-033) — hiding it would break
 * Home, not just remove a shell detail route.
 *
 * No preference is user-configurable here — the real control ("Sync my financial records") is
 * Account-tier and this contribution's own rules (settings-contribution.md rule 10) forbid reading
 * `ConsentRepository` directly — so this follows `unitSettingsContribution`'s precedent: one real,
 * static fact per row (the frozen enum sizes this module already validates against, BR-C3) rather
 * than an invented toggle (SC-011).
 */
fun netWorthSettingsContribution(): SettingsContribution =
    SettingsContribution(
        moduleKey = MODULE_KEY,
        title = R.string.settings_networth_title,
        summary = R.string.settings_networth_summary,
        order = 3,
        groups =
            listOf(
                SettingsGroup(
                    label = null,
                    rows =
                        listOf(
                            SettingsRow.Info(
                                key = "networth_sector_count",
                                label = R.string.settings_networth_sector_count_label,
                                description = R.string.settings_networth_sector_count_description,
                                value = flowOf(Sector.entries.size.toString()),
                            ),
                            SettingsRow.Info(
                                key = "networth_liability_type_count",
                                label = R.string.settings_networth_liability_type_count_label,
                                description = R.string.settings_networth_liability_type_count_description,
                                value = flowOf(LiabilityType.entries.size.toString()),
                            ),
                        ),
                ),
            ),
    )
