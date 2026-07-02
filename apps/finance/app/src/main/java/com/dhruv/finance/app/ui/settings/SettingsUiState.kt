package com.dhruv.finance.app.ui.settings

data class SettingsUiState(
    val isDegree: Boolean = true,
    val darkModePreference: String = "system",
    val decimalPrecision: Int = 4,
    val formatLocale: String = "international",
    val isHistoryLocked: Boolean = false,
    val historyPinCode: String = "",
    val isConverterEnabled: Boolean = true,
    val isDateEnabled: Boolean = true,
    val isFinanceEnabled: Boolean = true,
    val isTimeEnabled: Boolean = true,
    val calculatorColor: String = "cyan",
    val converterColor: String = "purple",
    val dateColor: String = "coral",
    val financeColor: String = "amber",
    val timeColor: String = "teal",
)
