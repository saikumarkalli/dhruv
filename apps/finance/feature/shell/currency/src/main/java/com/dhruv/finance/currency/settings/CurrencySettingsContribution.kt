package com.dhruv.finance.currency.settings

import com.dhruv.finance.currency.R
import com.dhruv.settings.SettingsRepository
import com.dhruv.settings.contribution.ChoiceOption
import com.dhruv.settings.contribution.SettingsContribution
import com.dhruv.settings.contribution.SettingsGroup
import com.dhruv.settings.contribution.SettingsRow
import kotlinx.coroutines.flow.flowOf

private const val MODULE_KEY = "currency"
private const val DAILY_RATES_ALERT_KEY = "alert_daily_rates"
private const val DAILY_RATES_TIME_KEY = "daily_rates_alert_time"
private const val DEFAULT_ALERT_TIME = "09:00"

// Mirrors CurrencyViewModel.availableCurrencies.size — kept in sync manually (same convention as
// FeatureFlagAssetLoaderTest's hand-mirrored JSON) since this module has no persisted preference
// of its own yet to derive the count from.
private const val SUPPORTED_CURRENCY_COUNT = 13

/**
 * The currency converter's Settings entry (T035). `daily_rates` (T090) is the first real alert
 * control, establishing the convention `SET-BR-006` measures — its `Toggle.key` names the channel
 * it controls (`alert_<channelId>`, matching [com.dhruv.core.notification.NotificationChannelRegistry]).
 *
 * A metals-rates alert deliberately does **not** exist here — that feature is designed but not
 * built (see the currency-metals-notification plan), so per FR-031 its row stays absent rather
 * than present-and-inert until it actually ships.
 */
fun currencySettingsContribution(settingsRepository: SettingsRepository): SettingsContribution =
    SettingsContribution(
        moduleKey = MODULE_KEY,
        title = R.string.settings_currency_title,
        summary = R.string.settings_currency_summary,
        order = 1,
        // Optional (FR-032): a shell detail route, not a tab's content — safe to hide.
        optional = true,
        groups =
            listOf(
                SettingsGroup(
                    label = null,
                    rows =
                        listOf(
                            SettingsRow.Info(
                                key = "supported_currency_count",
                                label = R.string.settings_currency_supported_label,
                                description = R.string.settings_currency_supported_description,
                                value = flowOf(SUPPORTED_CURRENCY_COUNT.toString()),
                            ),
                            SettingsRow.Toggle(
                                key = DAILY_RATES_ALERT_KEY,
                                label = R.string.settings_currency_daily_rates_label,
                                description = R.string.settings_currency_daily_rates_description,
                                value = settingsRepository.isToolEnabled(DAILY_RATES_ALERT_KEY, defaultValue = false),
                                onChange = { settingsRepository.setToolEnabled(DAILY_RATES_ALERT_KEY, it) },
                            ),
                            SettingsRow.Choice(
                                key = DAILY_RATES_TIME_KEY,
                                label = R.string.settings_currency_daily_rates_time_label,
                                description = R.string.settings_currency_daily_rates_time_description,
                                options =
                                    listOf(
                                        ChoiceOption("07:00", R.string.settings_currency_time_7am),
                                        ChoiceOption("09:00", R.string.settings_currency_time_9am),
                                        ChoiceOption("20:00", R.string.settings_currency_time_8pm),
                                    ),
                                selected = settingsRepository.toolStringValue(DAILY_RATES_TIME_KEY, DEFAULT_ALERT_TIME),
                                onSelect = { settingsRepository.setToolStringValue(DAILY_RATES_TIME_KEY, it) },
                            ),
                        ),
                ),
            ),
    )
