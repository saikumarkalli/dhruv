package com.dhruv.finance.assistant

import com.dhruv.settings.AppSettings
import com.dhruv.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * Hand-written [SettingsRepository] test double, scoped to this module's own tests — no mocking
 * library in this project's catalog, and no shared testFixtures source set exists across modules,
 * so each module keeps its own minimal fake (same convention as
 * `apps/finance/app/src/test/.../FakeSettingsRepository.kt`).
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

    override val isDegree: StateFlow<Boolean> = MutableStateFlow(true)
    override val darkModePreference: StateFlow<String> = MutableStateFlow("system")
    override val decimalPrecision: StateFlow<Int> = MutableStateFlow(4)
    override val isHistoryLocked: StateFlow<Boolean> = MutableStateFlow(false)
    override val historyPinCode: StateFlow<String> = MutableStateFlow("")
    override val formatLocale: StateFlow<String> = MutableStateFlow("international")

    override fun setDegree(degree: Boolean) = Unit

    override fun setDarkModePreference(preference: String) = Unit

    override fun setDecimalPrecision(precision: Int) = Unit

    override fun setHistoryLocked(locked: Boolean) = Unit

    override fun setHistoryPinCode(pin: String) = Unit

    override fun setFormatLocale(locale: String) = Unit

    private val toolEnabledMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())

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

    private val moduleEnabledMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())

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
