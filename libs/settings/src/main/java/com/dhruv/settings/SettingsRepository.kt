package com.dhruv.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Unified settings repository.
 *
 * ### New API (Phase 3)
 * - [observe] — a single [Flow] of [AppSettings] snapshots; use in SettingsViewModel / MainActivity.
 * - [update] — atomic update via a copy-lambda; writes only the keys that changed.
 * - [clearGeminiKey] — wipes the encrypted Gemini API key.
 *
 * ### Legacy API
 * All StateFlow properties and setX() methods from the original com.example.data.SettingsRepository
 * are preserved here so that CalculatorViewModel, ConverterScreen, DateScreen, FinanceScreen,
 * SettingsSectionDetailSheet, and SettingsScreen continue to compile without change.
 */
interface SettingsRepository {

    // ── New Phase-3 API ───────────────────────────────────────────────────────

    /** Emits an [AppSettings] snapshot whenever any value changes. Never errors. */
    fun observe(): Flow<AppSettings>

    /**
     * Atomically applies [block] to the current [AppSettings] and persists the result.
     * Only writes keys whose values actually changed.
     */
    suspend fun update(block: AppSettings.() -> AppSettings)

    /** Removes the Gemini API key from the encrypted DataStore. */
    suspend fun clearGeminiKey()

    // ── Legacy StateFlow properties (same semantics as old SettingsRepository) ─

    val isDegree: StateFlow<Boolean>
    val darkModePreference: StateFlow<String>
    val decimalPrecision: StateFlow<Int>
    val isHistoryLocked: StateFlow<Boolean>
    val historyPinCode: StateFlow<String>
    val calculatorColor: StateFlow<String>
    val converterColor: StateFlow<String>
    val dateColor: StateFlow<String>
    val financeColor: StateFlow<String>
    val formatLocale: StateFlow<String>
    val isConverterEnabled: StateFlow<Boolean>
    val isDateEnabled: StateFlow<Boolean>
    val isFinanceEnabled: StateFlow<Boolean>
    val timeColor: StateFlow<String>
    val isTimeEnabled: StateFlow<Boolean>

    // ── Legacy setter methods ─────────────────────────────────────────────────

    fun setDegree(degree: Boolean)
    fun setDarkModePreference(preference: String)
    fun setDecimalPrecision(precision: Int)
    fun setHistoryLocked(locked: Boolean)
    fun setHistoryPinCode(pin: String)
    fun setCalculatorColor(color: String)
    fun setConverterColor(color: String)
    fun setDateColor(color: String)
    fun setFinanceColor(color: String)
    fun setFormatLocale(locale: String)
    fun setConverterEnabled(enabled: Boolean)
    fun setDateEnabled(enabled: Boolean)
    fun setFinanceEnabled(enabled: Boolean)
    fun setTimeColor(color: String)
    fun setTimeEnabled(enabled: Boolean)

    /** Returns a [Flow] indicating whether a specific tool within a section is enabled. */
    fun isToolEnabled(key: String, defaultValue: Boolean = true): Flow<Boolean>

    /** Persists the enabled/disabled state of a specific tool within a section. */
    fun setToolEnabled(key: String, enabled: Boolean)
}
