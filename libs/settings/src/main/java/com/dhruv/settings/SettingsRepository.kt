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
 * The remaining StateFlow properties and setX() methods below are what's left of the original
 * com.example.data.SettingsRepository surface still actually consumed today. The per-section
 * accent colors (`calculatorColor`/`converterColor`/`dateColor`/`financeColor`/`timeColor`) and the
 * per-tab enable flags (`isConverterEnabled`/`isDateEnabled`/`isFinanceEnabled`/`isTimeEnabled`)
 * were removed 0b.5 (T110, `SET-BR-009`/SC-005 orphan-preference audit) — their consumers
 * (ConverterScreen/DateScreen/FinanceScreen/SettingsSectionDetailSheet, `SectionTheme`'s per-domain
 * accents, the old 5-tab pager's per-tab visibility toggles) were already deleted by ADR-0024's
 * single-global-accent/4-tab redesign; these accessors had become dead code with no caller anywhere
 * in the app, verified by full-repo grep before removal. Their underlying DataStore keys
 * (`color_calculator` etc., `is_converter_enabled` etc.) are removed too (Article IX governs
 * renaming, not deletion of a key nothing writes or reads anymore) — any bytes already persisted
 * for an existing install are simply orphaned in the DataStore file, never read again.
 */
interface SettingsRepository {
    // ── New Phase-3 API ───────────────────────────────────────────────────────

    /** Emits an [AppSettings] snapshot whenever any value changes. Never errors. */
    fun observe(): Flow<AppSettings>

    /**
     * The [AppSettings] snapshot available **synchronously**, with no coroutine dispatch — needed
     * so the app-lock gate's cold-start decision (`contracts/app-lock-gate.md` §1 rule 1) can use
     * the real `biometricEnabled` value on the very first composed frame, rather than
     * [AppSettings]'s all-false default while an async DataStore read is still in flight. That gap
     * would otherwise be exactly the "flash of unlocked content" the gate exists to prevent
     * (gate §2 rule 8). [geminiApiKey] is deliberately left `null` here (not gate/theme-relevant,
     * not worth a second blocking encrypted-store read on the startup path).
     */
    fun currentSnapshot(): AppSettings

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
    val formatLocale: StateFlow<String>

    // ── Legacy setter methods ─────────────────────────────────────────────────

    fun setDegree(degree: Boolean)

    fun setDarkModePreference(preference: String)

    fun setDecimalPrecision(precision: Int)

    fun setHistoryLocked(locked: Boolean)

    fun setHistoryPinCode(pin: String)

    fun setFormatLocale(locale: String)

    /** Returns a [Flow] indicating whether a specific tool within a section is enabled. */
    fun isToolEnabled(
        key: String,
        defaultValue: Boolean = true,
    ): Flow<Boolean>

    /** Persists the enabled/disabled state of a specific tool within a section. */
    fun setToolEnabled(
        key: String,
        enabled: Boolean,
    )

    /** Generic per-key string preference, same "tool_"-keyed convention as [isToolEnabled] —
     * for a module's own small `Choice` preferences that don't warrant a dedicated [AppSettings]
     * field (e.g. a delivery-time choice). */
    fun toolStringValue(
        key: String,
        defaultValue: String,
    ): Flow<String>

    /** Persists [toolStringValue]'s value. */
    fun setToolStringValue(
        key: String,
        value: String,
    )

    /**
     * Whether the optional module named [moduleKey] is turned on (FR-032, `module_enabled_<moduleKey>`,
     * data-model.md §3). Defaults to enabled — a module with no stored flag yet has never been turned
     * off. Generic over any `moduleKey` so a stored flag for a module later removed from the build is
     * simply inert, never an orphan UI entry (contract's own resolution never reads this key directly).
     */
    fun isModuleEnabled(
        moduleKey: String,
        defaultValue: Boolean = true,
    ): Flow<Boolean>

    /** Persists [moduleKey]'s on/off state. Touches only this key — a module's other stored
     * preferences are untouched by turning it off, so re-enabling restores them (`SET-BR-005`). */
    suspend fun setModuleEnabled(
        moduleKey: String,
        enabled: Boolean,
    )
}
