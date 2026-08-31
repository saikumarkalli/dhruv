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
    val FORMAT_LOCALE = stringPreferencesKey("format_locale")

    // Removed 0b.5 (T110, SET-BR-009/SC-005 orphan-preference audit): COLOR_CALCULATOR,
    // COLOR_CONVERTER, COLOR_DATE, COLOR_FINANCE, COLOR_TIME (per-section accents, retired by
    // ADR-0024's single global accent) and IS_CONVERTER_ENABLED, IS_DATE_ENABLED,
    // IS_FINANCE_ENABLED, IS_TIME_ENABLED (per-tab visibility, retired with the old 5-tab pager) —
    // zero consumers anywhere in the app, verified by full-repo grep before removal. See
    // SettingsRepository.kt's class doc for the full removal rationale.

    // New keys added by Phase 3
    val FONT_FAMILY = stringPreferencesKey("font_family")
    val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    val SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
    val ACCENT_COLOR_HEX = stringPreferencesKey("accent_color_hex")

    // New keys added by Phase 0b (Settings control plane, data-model.md §3) — appending only,
    // never renaming an existing key above (Article IX).
    val APP_LOCK_TIMEOUT = stringPreferencesKey("app_lock_timeout")
    val HIDE_AMOUNTS = booleanPreferencesKey("hide_amounts")
    val NOTIFICATIONS_MASTER = booleanPreferencesKey("notifications_master")
    val ASSISTANT_CONSENT_GRANTED = booleanPreferencesKey("assistant_consent_granted")

    /** One key per optional module, e.g. `module_enabled_currency` (data-model.md §3). */
    fun moduleEnabled(moduleKey: String) = booleanPreferencesKey("module_enabled_$moduleKey")

    // Encrypted store key (lives in "secure_settings" EncryptedDataStore, not "app_settings")
    val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
}
