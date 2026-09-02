package com.dhruv.finance.onboarding

import androidx.lifecycle.viewModelScope
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.FeatureViewModel
import com.dhruv.finance.data.tracker.auth.AuthRepository
import com.dhruv.finance.data.tracker.auth.ConsentRepository
import com.dhruv.finance.data.tracker.auth.SessionState
import com.dhruv.finance.data.tracker.auth.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns the A2 (sign-in) -> A3 (DPDP consent) -> A4 (empty start) state machine (functional spec
 * §5 Group A; QA catalog §2 `ONB-*`).
 *
 * [uiState] carries exactly one case per screen ([OnboardingUiState]). [exitToShell] is a
 * separate, orthogonal signal for leaving the onboarding flow entirely once it is done — either
 * straight from A2 ("Use offline", ONB-FLOW-003) or from A3 once the user already has tracker
 * data (the shell-or-A4 branch of [onConsentContinue]). It is a distinct `StateFlow` rather than a
 * fourth [OnboardingUiState] case because it answers a different question ("should the host still
 * be showing onboarding at all?") than "which of the three onboarding screens is up right now?".
 *
 * All three `_exitToShell.value = true` sites (offline at A2, EmptyStart's "Skip for now" at A4 —
 * [onSkipEmptyStart] — and, once Phase 2 lands, the has-data branch of [onConsentContinue]) also
 * persist [ConsentRepository.setHasCompletedOnboarding] (Task 3 decision 1) — the flag
 * `MainActivity` reads on cold launch to skip onboarding entirely for a returning user. Merely
 * landing on [OnboardingUiState.EmptyStart] does NOT set it: a user who hasn't yet tapped "Skip
 * for now" sees A4 again on relaunch, an honest Phase-1 limitation rather than a false "completed"
 * signal.
 */
class OnboardingViewModel(
    crashReporter: CrashReporter,
    private val sessionStore: SessionStore,
    private val consentRepository: ConsentRepository,
    private val authRepository: AuthRepository,
) : FeatureViewModel(crashReporter, "onboarding") {
    private val _uiState = MutableStateFlow(initialUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _exitToShell = MutableStateFlow(false)
    val exitToShell: StateFlow<Boolean> = _exitToShell.asStateFlow()

    /**
     * Cold install / signed-out resumes at A2. A session that is already [SessionState.Active] —
     * e.g. the process died right after Google sign-in succeeded but before A3/A4 finished — skips
     * straight to A3 instead of asking the user to sign in twice.
     */
    private fun initialUiState(): OnboardingUiState =
        when (sessionStore.state.value) {
            is SessionState.Active -> consentScreenState()
            SessionState.SignedOut, SessionState.Expired -> OnboardingUiState.SignIn
        }

    private fun consentScreenState(isSubmitting: Boolean = false) =
        OnboardingUiState.Consent(switches = consentRepository.state.value, isSubmitting = isSubmitting)

    /**
     * ONB-FLOW-002 / ONB-BR-001. The Compose layer performs the Credential Manager call (Android
     * API, not testable from a ViewModel test) and hands the raw Google ID token here. Sign-in is
     * pre-consent: the only dependency touched on this path is [AuthRepository] — no consent
     * repository read/write happens before A3 is shown.
     *
     * A `suspend` function returning [Result] rather than a fire-and-forget call routed through
     * [reportFeatureError] (final whole-branch review — a failed backend sign-in used to set
     * [featureError], which [FeatureHost][com.dhruv.core.ui.FeatureHost] renders as a permanent,
     * no-retry error card in place of [SignInScreen][com.dhruv.finance.onboarding.SignInScreen]
     * itself — the very first screen a user sees, on an Activity-scoped ViewModel that's never
     * recreated, so one failed attempt (network blip, bad `SUPABASE_URL`) permanently bricked
     * onboarding with no in-app recovery). A network/auth failure here is exactly as retryable as
     * the Credential Manager failures [SignInScreen] already handles inline — this now lets the
     * caller do the same, instead of escalating to a feature crash. [authRepository] itself is
     * still the only thing this touches on failure or success — reported to [crashReporter] for
     * observability, never to [featureError].
     *
     * [rawNonce] is threaded straight through to [AuthRepository.signInWithGoogleIdToken] — see
     * that interface's doc comment for why GoTrue needs it (this Supabase project enforces nonce
     * checking on the Google id_token exchange).
     */
    suspend fun onGoogleIdTokenReceived(
        idToken: String,
        rawNonce: String,
    ): Result<Unit> =
        authRepository
            .signInWithGoogleIdToken(idToken, rawNonce)
            .onSuccess { _uiState.value = consentScreenState() }
            .onFailure { crashReporter.recordException(it) }

    /**
     * ONB-FLOW-003: skips straight to the shell; A3/A4 are never shown. Persists
     * [ConsentRepository.setHasCompletedOnboarding] first (Task 3 decision 1) so `MainActivity`
     * never re-shows onboarding on a later cold launch — reaching [OnboardingUiState.EmptyStart]
     * does NOT set this flag; only this site and [onConsentContinue]'s has-data branch do.
     */
    fun onUseOfflineSelected() {
        viewModelScope.launch(exceptionHandler) {
            consentRepository.setHasCompletedOnboarding(true)
            _exitToShell.value = true
        }
    }

    /**
     * ONB-BR-004 / ONB-BR-005: persists immediately via [ConsentRepository] — [uiState] never
     * buffers a switch value only in memory; it is re-read from the repository after every write
     * so it always mirrors the single source of truth.
     */
    fun onConsentSwitchToggled(
        switch: ConsentSwitch,
        value: Boolean,
    ) {
        viewModelScope.launch(exceptionHandler) {
            when (switch) {
                ConsentSwitch.SYNC_FINANCIAL_RECORDS -> consentRepository.setSyncFinancialRecords(value)
                ConsentSwitch.READ_TRANSACTION_SMS -> consentRepository.setReadTransactionSms(value)
                ConsentSwitch.ASK_DHRUV_ABOUT_MONEY -> consentRepository.setAskDhruvAboutMoney(value)
            }
            val current = _uiState.value
            if (current is OnboardingUiState.Consent) {
                _uiState.value = current.copy(switches = consentRepository.state.value)
            }
        }
    }

    /**
     * ONB-FLOW-002 -> shell-or-A4 transition. ONB-BR-002: declining every switch still proceeds —
     * consent gates what syncs, never whether the user can continue.
     */
    fun onConsentContinue() {
        viewModelScope.launch(exceptionHandler) {
            _uiState.value = consentScreenState(isSubmitting = true)

            if (hasAccountOrHolding()) {
                consentRepository.setHasCompletedOnboarding(true)
                _exitToShell.value = true
            } else {
                _uiState.value = OnboardingUiState.EmptyStart(hasAccountOrHolding = false)
            }
        }
    }

    // Phase 2 — no accounts/holdings repository exists yet. A4's exit criterion (functional spec
    // §5 Group A, the footnote right after the A4 row: "at least one account or one holding
    // exists -> Home becomes the landing tab") genuinely can't be implemented until that
    // repository lands; stubbed false so a signed-in user with zero accounts/holdings always sees
    // A4 rather than faking a real check. Will become a real (likely suspending) repository call
    // in Phase 2, so it stays a function rather than a constant despite always returning false today.
    @Suppress("FunctionOnlyReturningConstant")
    private fun hasAccountOrHolding(): Boolean = false

    /**
     * A4's exit affordance ("Skip for now" — final whole-branch review, Fix 1 / Critical).
     * [hasAccountOrHolding] is honestly stubbed `false` for all of Phase 1 (see above), so
     * [onConsentContinue] always lands every signed-in user on [OnboardingUiState.EmptyStart] —
     * without this, that screen had no way out at all (no [androidx.activity.OnBackPressedCallback]
     * anywhere in the app, and its two task rows dispatch [com.dhruv.core.navigation.NavigationDispatcher]
     * targets that are silently dropped while onboarding is showing, since `AppShell` — the only
     * collector — isn't composed yet). Same shape as [onUseOfflineSelected]: persist
     * [ConsentRepository.setHasCompletedOnboarding] first so a later cold launch never re-shows
     * onboarding, then flip [exitToShell].
     */
    fun onSkipEmptyStart() {
        viewModelScope.launch(exceptionHandler) {
            consentRepository.setHasCompletedOnboarding(true)
            _exitToShell.value = true
        }
    }

    /**
     * Un-latches [exitToShell] (Fix 4 — final whole-branch review). [exitToShell] is a one-way
     * latch by design (Task 3 decision 2) and this ViewModel is a single Activity-scoped instance
     * (`koinViewModel()` resolved once in `MainActivity`'s `setContent`, reused for the whole
     * process) — so without this, a later Settings > Privacy "Delete my account"
     * ([com.dhruv.finance.data.tracker.auth.TrackerAccountRepository.deleteMyAccount], which resets
     * [ConsentRepository.setHasCompletedOnboarding] to `false` server-side-confirmed) would leave
     * `MainActivity`'s onboarding gate (`!hasCompletedOnboarding && !exitToShell`) permanently
     * latched on the stale `true`, still showing the now-deleted-account's shell. `MainActivity`
     * calls this once it observes [ConsentRepository]'s `hasCompletedOnboarding` flip back to
     * `false`.
     *
     * Also re-derives [uiState] via [initialUiState] (final whole-branch review, fix-wave re-review
     * — un-latching [exitToShell] alone left [uiState] stuck on whatever screen it was last set to,
     * typically [OnboardingUiState.EmptyStart] from before the user deleted their account; landing
     * a freshly-signed-out user back on a signed-in-only screen with no way to reach [SignIn][OnboardingUiState.SignIn]).
     * By the time `MainActivity` observes `hasCompletedOnboarding` flip to `false`, deleting the
     * account has already cleared the session
     * ([TrackerAccountRepository.deleteMyAccount][com.dhruv.finance.data.tracker.auth.TrackerAccountRepository.deleteMyAccount]
     * clears the session before resetting the flag), so [initialUiState] correctly resolves to
     * [OnboardingUiState.SignIn] here, not a stale mid-flow screen.
     */
    fun resetForNewOnboardingSession() {
        _exitToShell.value = false
        _uiState.value = initialUiState()
    }
}
