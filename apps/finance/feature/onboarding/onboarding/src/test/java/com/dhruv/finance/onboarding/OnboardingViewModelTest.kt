package com.dhruv.finance.onboarding

import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.finance.data.tracker.auth.ConsentState
import com.dhruv.finance.data.tracker.auth.SessionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Covers QA catalog §2 (`ONB-*`, `2026-08-09-qa-test-scenario-catalog.md`) rows owned by this
 * ViewModel:
 *
 * - ONB-FLOW-001..004 and ONB-BR-001, ONB-BR-002, ONB-BR-004, ONB-BR-005 — exercised directly
 *   below.
 * - ONB-BR-003 ("Sync my financial records" off -> a tracker repository call is rejected before
 *   dispatch) is Backend-owned and lives entirely in `ConsentInterceptor` — already covered by
 *   Task 1's `ConsentInterceptorTest` (`:apps:finance:data`). Nothing in `OnboardingViewModel`
 *   makes a PostgREST call, so there is nothing to re-test at this layer.
 * - ONB-BR-006 (a tracker screen degrading to `SignedOutCard` the instant sync consent is turned
 *   off) is owned by each tracker screen's own ViewModel (Home/Money/Plan-live/Insights, observing
 *   `ConsentRepository.state` directly) — `OnboardingViewModel` has no tracker-content screens of
 *   its own to degrade.
 * - ONB-FLOW-005 chains ONB-BR-005/006 with the Settings › Privacy erasure actions (Task 4 scope);
 *   ONB-BR-007/008/009 are deferred/Backend-owned per the brief. None are exercised here.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private fun viewModel(
        sessionStore: FakeSessionStore = FakeSessionStore(),
        consentRepository: FakeConsentRepository = FakeConsentRepository(),
        authRepository: FakeAuthRepository = FakeAuthRepository(),
    ) = OnboardingViewModel(
        NoOpCrashReporter,
        sessionStore,
        consentRepository,
        authRepository,
    )

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    // ONB-FLOW-001
    @Test
    fun `cold install with no session shows SignIn`() {
        val vm = viewModel(sessionStore = FakeSessionStore(SessionState.SignedOut))
        assertEquals(OnboardingUiState.SignIn, vm.uiState.value)
    }

    // Not a catalogued ONB row — documents initialUiState's process-death-resilience branch.
    @Test
    fun `initial state resumes at Consent when a session is already active`() {
        val vm =
            viewModel(
                sessionStore = FakeSessionStore(SessionState.Active("user-1", "user@example.com")),
            )
        assertEquals(OnboardingUiState.Consent(ConsentState(), isSubmitting = false), vm.uiState.value)
    }

    // ONB-BR-001
    @Test
    fun `sign-in touches only AuthRepository, never the consent repository`() =
        runTest(dispatcher) {
            val consentRepository = FakeConsentRepository()
            val authRepository = FakeAuthRepository()
            val vm = viewModel(consentRepository = consentRepository, authRepository = authRepository)

            vm.onGoogleIdTokenReceived("raw-id-token", "raw-nonce")
            advanceUntilIdle()

            assertEquals(1, authRepository.callCount)
            assertEquals(0, consentRepository.totalSetterCallCount)
        }

    // ONB-FLOW-002
    @Test
    fun `sign-in success transitions SignIn to Consent with the repository's current switches`() =
        runTest(dispatcher) {
            val consentRepository = FakeConsentRepository()
            val vm = viewModel(consentRepository = consentRepository, authRepository = FakeAuthRepository())

            vm.onGoogleIdTokenReceived("raw-id-token", "raw-nonce")
            advanceUntilIdle()

            assertEquals(OnboardingUiState.Consent(ConsentState(), isSubmitting = false), vm.uiState.value)
        }

    // Not a catalogued ONB row — defensive coverage for the Result#failure branch. Rewritten by the
    // final whole-branch review's fix wave: onGoogleIdTokenReceived used to route a failure through
    // reportFeatureError, which FeatureHost renders as a permanent, no-retry error card in place of
    // SignInScreen — bricking the very first screen on this Activity-scoped, never-recreated
    // ViewModel. It's a suspend function returning Result now, and the caller (SignInScreen) shows
    // retryable inline copy instead — this test proves featureError is never touched.
    @Test
    fun `sign-in failure returns Result failure without touching feature-error state`() =
        runTest(dispatcher) {
            val failure = IllegalStateException("network down")
            val vm = viewModel(authRepository = FakeAuthRepository(Result.failure(failure)))

            val result = vm.onGoogleIdTokenReceived("raw-id-token", "raw-nonce")

            assertTrue(result.isFailure)
            assertEquals(OnboardingUiState.SignIn, vm.uiState.value)
            assertEquals(null, vm.featureError.value)
        }

    // ONB-FLOW-003
    @Test
    fun `use offline selected exits to shell without ever showing Consent or EmptyStart`() =
        runTest(dispatcher) {
            val vm = viewModel()

            vm.onUseOfflineSelected()
            advanceUntilIdle()

            assertTrue(vm.exitToShell.value)
            assertEquals(OnboardingUiState.SignIn, vm.uiState.value)
        }

    // Task 3 decision 1 — no dedicated ONB/DAT catalog row exists for this specific flag.
    @Test
    fun `use offline selected persists hasCompletedOnboarding before exiting to shell`() =
        runTest(dispatcher) {
            val consentRepository = FakeConsentRepository()
            val vm = viewModel(consentRepository = consentRepository)

            vm.onUseOfflineSelected()
            advanceUntilIdle()

            assertEquals(1, consentRepository.setHasCompletedOnboardingCallCount)
            assertTrue(consentRepository.state.value.hasCompletedOnboarding)
        }

    // ONB-BR-002
    @Test
    fun `declining every consent switch still lets Continue proceed to EmptyStart`() =
        runTest(dispatcher) {
            val authRepository = FakeAuthRepository()
            val vm = viewModel(authRepository = authRepository)
            vm.onGoogleIdTokenReceived("raw-id-token", "raw-nonce")
            advanceUntilIdle()

            // No switches toggled — every A3 consent is left at its default (off).
            vm.onConsentContinue()
            advanceUntilIdle()

            assertEquals(OnboardingUiState.EmptyStart(hasAccountOrHolding = false), vm.uiState.value)
            assertFalse(vm.exitToShell.value)
        }

    // ONB-BR-002 (via A4's skip action) — Continue -> EmptyStart above is real, intended behavior
    // (A4 IS meant to be shown); this covers the escape hatch A4 itself needed, since it had no
    // exit affordance at all (final whole-branch review, Fix 1 / Critical). A signed-in user who
    // reaches EmptyStart must still be able to reach the shell.
    @Test
    fun `skipping EmptyStart exits to shell and persists hasCompletedOnboarding`() =
        runTest(dispatcher) {
            val consentRepository = FakeConsentRepository()
            val vm = viewModel(consentRepository = consentRepository, authRepository = FakeAuthRepository())
            vm.onGoogleIdTokenReceived("raw-id-token", "raw-nonce")
            advanceUntilIdle()
            vm.onConsentContinue()
            advanceUntilIdle()
            assertEquals(OnboardingUiState.EmptyStart(hasAccountOrHolding = false), vm.uiState.value)

            vm.onSkipEmptyStart()
            advanceUntilIdle()

            assertTrue(vm.exitToShell.value)
            assertEquals(1, consentRepository.setHasCompletedOnboardingCallCount)
            assertTrue(consentRepository.state.value.hasCompletedOnboarding)
        }

    // Fix 4 (final whole-branch review) — exitToShell is an Activity-scoped, one-way latch
    // (OnboardingViewModel doc comment); this proves the escape hatch MainActivity uses after a
    // Settings > Privacy "Delete my account" (TrackerAccountRepositoryImpl.deleteMyAccount resets
    // ConsentRepository.hasCompletedOnboarding to false server-side-confirmed) to un-latch it, so
    // the onboarding gate re-shows onboarding instead of staying pinned to a deleted account's shell.
    @Test
    fun `resetForNewOnboardingSession flips exitToShell back to false`() =
        runTest(dispatcher) {
            val vm = viewModel()

            vm.onUseOfflineSelected()
            advanceUntilIdle()
            assertTrue(vm.exitToShell.value)

            vm.resetForNewOnboardingSession()

            assertFalse(vm.exitToShell.value)
        }

    // Not a catalogued ONB row — scoped re-review finding (final whole-branch review, fix-wave
    // re-review): resetForNewOnboardingSession used to reset only exitToShell, leaving uiState
    // stuck on whatever screen it was last set to (typically EmptyStart, left over from before the
    // account was deleted). A freshly signed-out user landed back on a signed-in-only A4 with no
    // way to reach SignIn anywhere in the app. Proves the post-account-deletion sequence: session
    // was Active, gets cleared (TrackerAccountRepository.deleteMyAccount clears the session before
    // resetting hasCompletedOnboarding), reset is called — uiState must resolve to SignIn, not stay
    // on the stale EmptyStart.
    @Test
    fun `resetForNewOnboardingSession re-derives uiState to SignIn after the session is cleared`() =
        runTest(dispatcher) {
            val sessionStore = FakeSessionStore(SessionState.Active("user-1", "user@example.com"))
            val consentRepository = FakeConsentRepository()
            val vm = viewModel(sessionStore = sessionStore, consentRepository = consentRepository)

            // Reach EmptyStart, same as a real signed-in user with no accounts/holdings yet.
            vm.onConsentContinue()
            advanceUntilIdle()
            assertEquals(OnboardingUiState.EmptyStart(hasAccountOrHolding = false), vm.uiState.value)

            // Simulates TrackerAccountRepository.deleteMyAccount()'s ordering: session cleared,
            // then MainActivity observes hasCompletedOnboarding flip and calls the reset.
            sessionStore.clear()
            vm.resetForNewOnboardingSession()

            assertEquals(OnboardingUiState.SignIn, vm.uiState.value)
            assertFalse(vm.exitToShell.value)
        }

    // ONB-BR-004 / ONB-BR-005
    @Test
    fun `toggling a switch calls straight through to ConsentRepository, never buffered only in ViewModel state`() =
        runTest(dispatcher) {
            val consentRepository = FakeConsentRepository()
            val vm = viewModel(consentRepository = consentRepository, authRepository = FakeAuthRepository())
            vm.onGoogleIdTokenReceived("raw-id-token", "raw-nonce")
            advanceUntilIdle()

            vm.onConsentSwitchToggled(ConsentSwitch.SYNC_FINANCIAL_RECORDS, true)
            advanceUntilIdle()

            assertEquals(1, consentRepository.setSyncFinancialRecordsCallCount)
            assertTrue(consentRepository.state.value.syncFinancialRecords)
            assertEquals(
                ConsentState(syncFinancialRecords = true),
                (vm.uiState.value as OnboardingUiState.Consent).switches,
            )
        }

    // ONB-BR-004 / ONB-BR-005 — sibling switches unaffected, mirrors Task 1's
    // ConsentRepositoryTest#`setting one switch does not affect the other two` from the
    // ViewModel's own entry point.
    @Test
    fun `toggling one switch leaves the other two untouched`() =
        runTest(dispatcher) {
            val consentRepository = FakeConsentRepository()
            val vm = viewModel(consentRepository = consentRepository, authRepository = FakeAuthRepository())
            vm.onGoogleIdTokenReceived("raw-id-token", "raw-nonce")
            advanceUntilIdle()

            vm.onConsentSwitchToggled(ConsentSwitch.READ_TRANSACTION_SMS, true)
            advanceUntilIdle()

            val switches = (vm.uiState.value as OnboardingUiState.Consent).switches
            assertTrue(switches.readTransactionSms)
            assertFalse(switches.syncFinancialRecords)
            assertFalse(switches.askDhruvAboutMoney)
        }

    // ONB-FLOW-004 — Phase 2 blocked (see OnboardingViewModel.hasAccountOrHolding's comment): the
    // actual "exits A4 to Home" transition can't be tested until the accounts/holdings repository
    // exists. This proves the stub's observable contract in the meantime.
    @Test
    fun `EmptyStart reports hasAccountOrHolding false until the accounts-holdings repository exists`() =
        runTest(dispatcher) {
            val vm = viewModel(authRepository = FakeAuthRepository())
            vm.onGoogleIdTokenReceived("raw-id-token", "raw-nonce")
            advanceUntilIdle()

            vm.onConsentContinue()
            advanceUntilIdle()

            assertEquals(OnboardingUiState.EmptyStart(hasAccountOrHolding = false), vm.uiState.value)
        }

    // Task 3 decision 1 — no dedicated ONB/DAT catalog row exists for this specific flag. Landing
    // on EmptyStart must NOT mark onboarding complete, so a user stuck there (Phase 2's
    // accounts/holdings repository doesn't exist yet) sees A4 again on the next cold launch rather
    // than being silently skipped past it.
    @Test
    fun `reaching EmptyStart does not persist hasCompletedOnboarding`() =
        runTest(dispatcher) {
            val consentRepository = FakeConsentRepository()
            val vm = viewModel(consentRepository = consentRepository, authRepository = FakeAuthRepository())
            vm.onGoogleIdTokenReceived("raw-id-token", "raw-nonce")
            advanceUntilIdle()

            vm.onConsentContinue()
            advanceUntilIdle()

            assertEquals(OnboardingUiState.EmptyStart(hasAccountOrHolding = false), vm.uiState.value)
            assertEquals(0, consentRepository.setHasCompletedOnboardingCallCount)
            assertFalse(consentRepository.state.value.hasCompletedOnboarding)
        }

    // Not a catalogued ONB row — documents the Consent.isSubmitting contract onConsentContinue relies
    // on. `hasAccountOrHolding()` is a synchronous Phase-2 stub (no suspension point), so the
    // isSubmitting=true write is conflated by StateFlow before any collector can observe it — that
    // window becomes genuinely observable only once Phase 2 replaces the stub with a real suspending
    // repository call. What's provable today is the field's resting value on either side of Continue.
    @Test
    fun `Consent isSubmitting starts false and Continue resolves to a settled, non-submitting screen`() =
        runTest(dispatcher) {
            val vm = viewModel(authRepository = FakeAuthRepository())
            vm.onGoogleIdTokenReceived("raw-id-token", "raw-nonce")
            advanceUntilIdle()
            assertFalse((vm.uiState.value as OnboardingUiState.Consent).isSubmitting)

            vm.onConsentContinue()
            advanceUntilIdle()

            assertEquals(OnboardingUiState.EmptyStart(hasAccountOrHolding = false), vm.uiState.value)
        }
}
