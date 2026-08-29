package com.dhruv.finance.app.ui.settings

import androidx.lifecycle.ViewModel
import com.dhruv.core.observability.CrashReporter
import com.dhruv.finance.data.tracker.auth.AuthRepository
import com.dhruv.finance.data.tracker.auth.ConsentRepository
import com.dhruv.finance.data.tracker.auth.ConsentState
import com.dhruv.finance.data.tracker.auth.SessionState
import com.dhruv.finance.data.tracker.auth.SessionStore
import com.dhruv.finance.data.tracker.auth.TrackerAccountRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Account tier (0b.2, US2): sign-in/sign-out/consent/erasure, wired directly to
 * [AuthRepository]/[SessionStore] — never to `com.dhruv.finance.onboarding` (research R6,
 * `SET-ARCH-003`: Settings must not reference a feature-module type). The Credential Manager call
 * itself is an Android-framework concern that can't be exercised from a ViewModel test (same
 * reasoning as `OnboardingViewModel.onGoogleIdTokenReceived`'s own doc comment) — it lives in
 * `AccountSettingsScreen.kt`, which hands this class the raw Google ID token.
 *
 * No `exportEnabled`/`hasRecords`-shaped property exists here on purpose (`SET-BR-023`): the
 * "Export my data" row was removed outright (T053, research R7) rather than conditionally shown,
 * so there is nothing here that could resurrect it by accident.
 */
class AccountSettingsViewModel(
    private val authRepository: AuthRepository,
    private val sessionStore: SessionStore,
    private val consentRepository: ConsentRepository,
    private val trackerAccountRepository: TrackerAccountRepository,
    private val crashReporter: CrashReporter,
) : ViewModel() {
    init {
        crashReporter.setModule("settings_account")
    }

    val sessionState: StateFlow<SessionState> = sessionStore.state

    val consentState: StateFlow<ConsentState> = consentRepository.state

    suspend fun setSyncFinancialRecords(enabled: Boolean) = consentRepository.setSyncFinancialRecords(enabled)

    suspend fun setReadTransactionSms(enabled: Boolean) = consentRepository.setReadTransactionSms(enabled)

    suspend fun setAskDhruvAboutMoney(enabled: Boolean) = consentRepository.setAskDhruvAboutMoney(enabled)

    /** `SET-FLOW-003`: delegates to [AuthRepository] only — the same one call
     * `OnboardingViewModel.onGoogleIdTokenReceived` makes, never routed through onboarding. */
    suspend fun onGoogleIdTokenReceived(
        idToken: String,
        rawNonce: String,
    ): Result<Unit> =
        authRepository.signInWithGoogleIdToken(idToken, rawNonce).onFailure { crashReporter.recordException(it) }

    /** `SET-FLOW-004`: clears the session/credentials only — never touches Room-backed calculator
     * data, because nothing here has a reference to it. */
    suspend fun signOut() = sessionStore.clear()

    /** `SET-BR-022`: returns the repository's [Result] unchanged — a failure is never swallowed
     * into a fabricated success, and this function does not track a "completed" flag that would
     * make the action unavailable for retry. */
    suspend fun deleteMyData(): Result<Unit> =
        trackerAccountRepository.deleteMyData().onFailure { crashReporter.recordException(it) }

    /** `SET-BR-021`/`SET-BR-022`: same shape as [deleteMyData]. The typed-confirmation requirement
     * (`DELETE_MY_ACCOUNT_CONFIRM_TEXT`) is enforced by the dialog before this is ever called. */
    suspend fun deleteMyAccount(): Result<Unit> =
        trackerAccountRepository.deleteMyAccount().onFailure { crashReporter.recordException(it) }
}
