package com.dhruv.finance.unit.settings

import com.dhruv.finance.unit.AreaUnit
import com.dhruv.finance.unit.LengthUnit
import com.dhruv.finance.unit.MassUnit
import com.dhruv.finance.unit.R
import com.dhruv.finance.unit.TemperatureUnit
import com.dhruv.settings.contribution.SettingsContribution
import com.dhruv.settings.contribution.SettingsGroup
import com.dhruv.settings.contribution.SettingsRow
import kotlinx.coroutines.flow.flowOf

private const val MODULE_KEY = "unit"

/**
 * The unit converter's Settings entry (T035) — no preference is user-configurable yet, so each
 * category (the "individual converters" the contract's grouping language refers to) gets its own
 * submodule group holding one real, static fact — its unit count, read from the same enums the
 * converter itself uses — rather than an invented toggle (SC-011).
 */
fun unitSettingsContribution(): SettingsContribution =
    SettingsContribution(
        moduleKey = MODULE_KEY,
        title = R.string.settings_unit_title,
        summary = R.string.settings_unit_summary,
        order = 2,
        // Optional (FR-032): a shell detail route, not a tab's content — safe to hide.
        optional = true,
        groups =
            listOf(
                unitCountGroup(R.string.settings_unit_category_length, LengthUnit.entries.size),
                unitCountGroup(R.string.settings_unit_category_mass, MassUnit.entries.size),
                unitCountGroup(R.string.settings_unit_category_temperature, TemperatureUnit.entries.size),
                unitCountGroup(R.string.settings_unit_category_area, AreaUnit.entries.size),
            ),
    )

private fun unitCountGroup(
    labelRes: Int,
    unitCount: Int,
) = SettingsGroup(
    label = labelRes,
    rows =
        listOf(
            SettingsRow.Info(
                key = "unit_count_$labelRes",
                label = R.string.settings_unit_units_available_label,
                description = R.string.settings_unit_units_available_description,
                value = flowOf(unitCount.toString()),
            ),
        ),
)
