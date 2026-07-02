package com.dhruv.finance.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.ui.theme.AppTheme
import com.dhruv.core.ui.theme.DhruvFont
import com.dhruv.settings.AppSettings
import com.dhruv.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for settings — collects [SettingsRepository.observe] as a [StateFlow] and exposes
 * update actions backed by [SettingsRepository.update] and [SettingsRepository.clearGeminiKey].
 *
 * Registered in Koin via [com.dhruv.finance.app.di.appModule] as `viewModel { SettingsViewModel(get(), get()) }`.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val crashReporter: CrashReporter,
) : ViewModel() {
    init {
        crashReporter.setModule("settings")
    }

    val settings: StateFlow<AppSettings> =
        settingsRepository
            .observe()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = AppSettings(),
            )

    fun setTheme(theme: AppTheme) = update { copy(theme = theme) }

    fun setAccentColorHex(hex: String) = update { copy(accentColorHex = hex) }

    fun setFont(font: DhruvFont) = update { copy(fontFamily = font) }

    fun setBiometricEnabled(enabled: Boolean) = update { copy(biometricEnabled = enabled) }

    fun saveGeminiKey(key: String) = update { copy(geminiApiKey = key.ifBlank { null }) }

    fun clearGeminiKey() {
        viewModelScope.launch {
            settingsRepository.clearGeminiKey()
        }
    }

    private fun update(block: AppSettings.() -> AppSettings) {
        viewModelScope.launch {
            settingsRepository.update(block)
        }
    }
}
