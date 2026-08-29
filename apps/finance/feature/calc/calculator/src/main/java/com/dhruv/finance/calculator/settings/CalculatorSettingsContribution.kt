package com.dhruv.finance.calculator.settings

import com.dhruv.finance.calculator.R
import com.dhruv.finance.data.HistoryRepository
import com.dhruv.settings.SettingsRepository
import com.dhruv.settings.contribution.ChoiceOption
import com.dhruv.settings.contribution.ConfirmSpec
import com.dhruv.settings.contribution.SettingsContribution
import com.dhruv.settings.contribution.SettingsGroup
import com.dhruv.settings.contribution.SettingsRow
import kotlinx.coroutines.flow.map
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private const val MODULE_KEY = "calculator"
private const val PREVIEW_SAMPLE = 12.3456789

/**
 * The calculator's Settings entry (T033) — number format, decimal precision (with a live preview),
 * angle mode, clear history. Migrates the existing rows, **reusing their existing preference keys**
 * unchanged (`SET-ARCH-006`, constitution IX) — this only moves where they're rendered from.
 */
fun calculatorSettingsContribution(
    settingsRepository: SettingsRepository,
    historyRepository: HistoryRepository,
): SettingsContribution =
    SettingsContribution(
        moduleKey = MODULE_KEY,
        title = R.string.settings_calculator_title,
        summary = R.string.settings_calculator_summary,
        order = 0,
        groups =
            listOf(
                SettingsGroup(
                    label = null,
                    rows =
                        listOf(
                            SettingsRow.Choice(
                                key = "format_locale",
                                label = R.string.settings_calculator_format_label,
                                description = R.string.settings_calculator_format_description,
                                options =
                                    listOf(
                                        ChoiceOption("international", R.string.settings_calculator_format_international),
                                        ChoiceOption("indian", R.string.settings_calculator_format_indian),
                                    ),
                                selected = settingsRepository.formatLocale,
                                onSelect = { settingsRepository.setFormatLocale(it) },
                            ),
                            SettingsRow.Stepper(
                                key = "decimal_precision",
                                label = R.string.settings_calculator_precision_label,
                                description = R.string.settings_calculator_precision_description,
                                value = settingsRepository.decimalPrecision,
                                range = 0..10,
                                step = 1,
                                onChange = { settingsRepository.setDecimalPrecision(it) },
                            ),
                            SettingsRow.Info(
                                key = "decimal_precision_preview",
                                label = R.string.settings_calculator_precision_preview_label,
                                description = R.string.settings_calculator_precision_description,
                                value = settingsRepository.decimalPrecision.map { precision -> formatPreview(precision) },
                            ),
                            SettingsRow.Choice(
                                key = "is_degree",
                                label = R.string.settings_calculator_angle_mode_label,
                                description = R.string.settings_calculator_angle_mode_description,
                                options =
                                    listOf(
                                        ChoiceOption("deg", R.string.settings_calculator_angle_deg),
                                        ChoiceOption("rad", R.string.settings_calculator_angle_rad),
                                    ),
                                selected = settingsRepository.isDegree.map { if (it) "deg" else "rad" },
                                onSelect = { settingsRepository.setDegree(it == "deg") },
                            ),
                            SettingsRow.Action(
                                key = "clear_history",
                                label = R.string.settings_calculator_clear_history_label,
                                description = R.string.settings_calculator_clear_history_description,
                                destructive = true,
                                confirm =
                                    ConfirmSpec(
                                        title = R.string.settings_calculator_clear_history_confirm_title,
                                        body = R.string.settings_calculator_clear_history_confirm_body,
                                        confirmLabel = R.string.settings_calculator_clear_history_confirm_action,
                                    ),
                                onInvoke = { runCatching { historyRepository.clear() } },
                            ),
                        ),
                ),
            ),
    )

private fun formatPreview(precision: Int): String {
    val pattern = if (precision > 0) "#." + "#".repeat(precision) else "#"
    return DecimalFormat(pattern, DecimalFormatSymbols(Locale.US)).format(PREVIEW_SAMPLE)
}
