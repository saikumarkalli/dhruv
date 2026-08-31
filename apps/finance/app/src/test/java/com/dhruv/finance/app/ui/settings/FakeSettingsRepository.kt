package com.dhruv.finance.app.ui.settings

import com.dhruv.settings.AppSettings
import com.dhruv.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * Hand-written [SettingsRepository] test double (no mocking library in this project's catalog —
 * every module's tests use fakes, matching `SessionStoreTest`'s own convention). Single mutable
 * [AppSettings] backing store for the Phase-3 API; legacy StateFlow properties are separate
 * `MutableStateFlow`s a test can write to directly, matching the real interface's split shape.
 */
class FakeSettingsRepository(
    initial: AppSettings = AppSettings(),
) : SettingsRepository {
    private val appSettings = MutableStateFlow(initial)

    override fun observe(): Flow<AppSettings> = appSettings

    override fun currentSnapshot(): AppSettings = appSettings.value

    override suspend fun update(block: AppSettings.() -> AppSettings) {
        appSettings.value = appSettings.value.block()
    }

    override suspend fun clearGeminiKey() {
        appSettings.value = appSettings.value.copy(geminiApiKey = null)
    }

    val degree = MutableStateFlow(true)
    val darkMode = MutableStateFlow("system")
    val precision = MutableStateFlow(4)
    val historyLocked = MutableStateFlow(false)
    val historyPin = MutableStateFlow("")
    val locale = MutableStateFlow("international")
    private val toolEnabledMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    override val isDegree: StateFlow<Boolean> = degree
    override val darkModePreference: StateFlow<String> = darkMode
    override val decimalPrecision: StateFlow<Int> = precision
    override val isHistoryLocked: StateFlow<Boolean> = historyLocked
    override val historyPinCode: StateFlow<String> = historyPin
    override val formatLocale: StateFlow<String> = locale

    override fun setDegree(degree: Boolean) {
        this.degree.value = degree
    }

    override fun setDarkModePreference(preference: String) {
        darkMode.value = preference
    }

    override fun setDecimalPrecision(precision: Int) {
        this.precision.value = precision
    }

    override fun setHistoryLocked(locked: Boolean) {
        historyLocked.value = locked
    }

    override fun setHistoryPinCode(pin: String) {
        historyPin.value = pin
    }

    override fun setFormatLocale(locale: String) {
        this.locale.value = locale
    }

    override fun isToolEnabled(
        key: String,
        defaultValue: Boolean,
    ): Flow<Boolean> = toolEnabledMap.map { it[key] ?: defaultValue }

    override fun setToolEnabled(
        key: String,
        enabled: Boolean,
    ) {
        toolEnabledMap.value = toolEnabledMap.value + (key to enabled)
    }

    private val toolStringMap = MutableStateFlow<Map<String, String>>(emptyMap())

    override fun toolStringValue(
        key: String,
        defaultValue: String,
    ): Flow<String> = toolStringMap.map { it[key] ?: defaultValue }

    override fun setToolStringValue(
        key: String,
        value: String,
    ) {
        toolStringMap.value = toolStringMap.value + (key to value)
    }

    /** Public so a Compose test can flip module state **synchronously**. Driving it through the
     * `suspend` [setModuleEnabled] from a test body needs `runBlocking`, which deadlocks against
     * `waitForIdle` on Robolectric's single main thread (observed: one such test took 25 minutes
     * before completing). Write this directly instead. */
    val moduleEnabledMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    override fun isModuleEnabled(
        moduleKey: String,
        defaultValue: Boolean,
    ): Flow<Boolean> = moduleEnabledMap.map { it[moduleKey] ?: defaultValue }

    override suspend fun setModuleEnabled(
        moduleKey: String,
        enabled: Boolean,
    ) {
        moduleEnabledMap.value = moduleEnabledMap.value + (moduleKey to enabled)
    }
}
