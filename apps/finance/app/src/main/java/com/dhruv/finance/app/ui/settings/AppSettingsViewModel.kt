package com.dhruv.finance.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhruv.core.observability.CrashReporter
import com.dhruv.settings.AppSettings
import com.dhruv.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** No device credential is enrolled — thrown from [AppSettingsViewModel.setAppLockEnabled]'s
 * failure result so the caller can show FR-022's "what to enrol" message specifically. */
class NoCredentialEnrolledException : Exception("No device credential is enrolled")

/**
 * The App tier's Security/Notifications area (0b.3). [hasEnrolledCredential] is injected as a plain
 * lambda rather than called directly against `BiometricManager` so `setAppLockEnabled` is unit
 * testable without Robolectric (`SET-BR-017`); the real check is wired at Koin registration time.
 */
class AppSettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val hasEnrolledCredential: () -> Boolean,
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
                initialValue = settingsRepository.currentSnapshot(),
            )

    /** FR-022, gate §2 rule 11: refused when no credential is enrolled — the switch never turns on
     * as though it were protecting something it can't. Disabling never needs the check (gate §1
     * rule 5 already makes the gate fall open with no credential, regardless of this preference). */
    suspend fun setAppLockEnabled(enabled: Boolean): Result<Unit> {
        if (enabled && !hasEnrolledCredential()) {
            return Result.failure(NoCredentialEnrolledException())
        }
        settingsRepository.update { copy(biometricEnabled = enabled) }
        return Result.success(Unit)
    }

    fun setAutoLockTimeout(timeoutId: String) {
        viewModelScope.launch {
            settingsRepository.update { copy(appLockTimeout = timeoutId) }
        }
    }

    fun setHideAmounts(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { copy(hideAmounts = enabled) }
        }
    }

    fun setNotificationsMaster(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { copy(notificationsMaster = enabled) }
        }
    }
}

/**
 * `SET-BR-010`/FR-026: the app-wide notification master suppresses every module's alerts
 * regardless of that module's own setting. Pure so every future alert-posting call site can share
 * this one check rather than re-deriving the AND.
 */
fun isAlertEffectivelyEnabled(
    notificationsMaster: Boolean,
    moduleAlertEnabled: Boolean,
): Boolean = notificationsMaster && moduleAlertEnabled
