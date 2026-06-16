package com.dhruv.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * DataStore preference key constants for the settings library.
 *
 * IMPORTANT: Key string values MUST NOT be changed — they match what was persisted in "app_settings"
 * by the old com.example.data.SettingsRepository so user data survives the migration.
 */
internal object SettingsKeys {
    // Legacy keys (same string values as old SettingsRepository.PreferencesKeys)
    val IS_DEGREE = booleanPreferencesKey("is_degree")
    val DARK_MODE = stringPreferencesKey("dark_mode")
    val DECIMAL_PRECISION = intPreferencesKey("decimal_precision")
    val IS_HISTORY_LOCKED = booleanPreferencesKey("is_history_locked")
    val HISTORY_PIN_CODE = stringPreferencesKey("history_pin_code")
    val COLOR_CALCULATOR = stringPreferencesKey("color_calculator")
    val COLOR_CONVERTER = stringPreferencesKey("color_converter")
    val COLOR_DATE = stringPreferencesKey("color_date")
    val COLOR_FINANCE = stringPreferencesKey("color_finance")
    val FORMAT_LOCALE = stringPreferencesKey("format_locale")
    val IS_CONVERTER_ENABLED = booleanPreferencesKey("is_converter_enabled")
    val IS_DATE_ENABLED = booleanPreferencesKey("is_date_enabled")
    val IS_FINANCE_ENABLED = booleanPreferencesKey("is_finance_enabled")
    val COLOR_TIME = stringPreferencesKey("color_time")
    val IS_TIME_ENABLED = booleanPreferencesKey("is_time_enabled")

    // New keys added by Phase 3
    val FONT_FAMILY = stringPreferencesKey("font_family")
    val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    val SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
    val ACCENT_COLOR_HEX = stringPreferencesKey("accent_color_hex")

    // Encrypted store key (lives in "secure_settings" EncryptedDataStore, not "app_settings")
    val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
}
