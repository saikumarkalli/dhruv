package com.dhruv.finance.app.ui.settings

/**
 * [accentColorHex] / [biometricEnabled] are the Phase-3 [com.dhruv.settings.AppSettings] fields
 * (ADR-0024 decision 2's global accent picker / the honest, real `biometricEnabled` preference)
 * surfaced here. The rest are the pre-existing calculator/history preferences, unchanged.
 */
data class SettingsUiState(
    val isDegree: Boolean = true,
    val darkModePreference: String = "system",
    val decimalPrecision: Int = 4,
    val formatLocale: String = "international",
    val isHistoryLocked: Boolean = false,
    val historyPinCode: String = "",
    val accentColorHex: String = "#F05A28",
    val biometricEnabled: Boolean = false,
)
