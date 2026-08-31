package com.dhruv.finance.app.ui.settings

import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.finance.data.tracker.auth.SessionState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `SET-FLOW-003`/`SET-FLOW-004`/`SET-BR-021`/`SET-BR-022`/`SET-BR-023` (004 US2, 0b.2).
 */
class AccountSettingsViewModelTest {
    private fun newVm(
        auth: FakeAuthRepositoryForAccount = FakeAuthRepositoryForAccount(),
        session: FakeSessionStoreForAccount = FakeSessionStoreForAccount(),
        consent: FakeConsentRepositoryForAccount = FakeConsentRepositoryForAccount(),
        tracker: FakeTrackerAccountRepository = FakeTrackerAccountRepository(),
    ) = AccountSettingsViewModel(auth, session, consent, tracker, NoOpCrashReporter)

    @Test
    fun `SET-FLOW-003 - signed out shows no placeholder identity`() {
        val vm = newVm(session = FakeSessionStoreForAccount(SessionState.SignedOut))
        assertEquals(SessionState.SignedOut, vm.sessionState.value)
    }

    @Test
    fun `SET-FLOW-003 - a successful google id token exchange delegates to AuthRepository only`() =
        runTest {
            val auth = FakeAuthRepositoryForAccount(result = Result.success(Unit))
            val vm = newVm(auth = auth)

            val result = vm.onGoogleIdTokenReceived("id-token", "raw-nonce")

            assertTrue(result.isSuccess)
            assertEquals(1, auth.callCount)
        }

    // NOTE: "sign-in never routes through the onboarding module" is deliberately NOT tested here.
    // It was, via reflection over this class's constructor parameter type names — which asserts on
    // an implementation detail, passes vacuously, and is strictly weaker than the guard that
    // already exists: `DependencyRulesTest.settings package must not reference a feature-module
    // type` checks every class in `app.ui.settings` against every feature module, not one
    // constructor against one string. Two overlapping guards where the weaker one looks
    // authoritative is worse than one real guard.

    @Test
    fun `SET-FLOW-004 - sign-out clears the session and touches nothing else`() =
        runTest {
            val session = FakeSessionStoreForAccount(SessionState.Active("uid", "a@b.com", null, null))
            val tracker = FakeTrackerAccountRepository()
            val vm = newVm(session = session, tracker = tracker)

            vm.signOut()

            assertEquals(1, session.clearCallCount)
            assertEquals(SessionState.SignedOut, vm.sessionState.value)
            // "Leaves on-device calculator history intact" is satisfied by construction: this
            // ViewModel holds no reference to HistoryRepository/Room at all, so signOut() has no
            // path to touch it — proven here by the tracker repository (a stand-in for "anything
            // else") never being called either.
            assertEquals(0, tracker.deleteMyDataCallCount)
        }

    @Test
    fun `SET-BR-022 - a failed data erasure reports failure, not a fabricated success`() =
        runTest {
            val failure = RuntimeException("offline")
            val tracker = FakeTrackerAccountRepository(deleteMyDataResult = Result.failure(failure))
            val vm = newVm(tracker = tracker)

            val result = vm.deleteMyData()

            assertTrue(result.isFailure)
            assertEquals(failure, result.exceptionOrNull())
        }

    @Test
    fun `SET-BR-021 SET-BR-022 - a failed account erasure reports failure and stays retryable`() =
        runTest {
            val failure = RuntimeException("rejected")
            val tracker = FakeTrackerAccountRepository(deleteMyAccountResult = Result.failure(failure))
            val vm = newVm(tracker = tracker)

            val first = vm.deleteMyAccount()
            assertTrue(first.isFailure)

            // "Stays available for retry" — calling it again is not blocked by any internal
            // one-shot/disabled state; the fake's own call counter proves a second attempt reaches
            // the repository at all.
            val second = vm.deleteMyAccount()
            assertTrue(second.isFailure)
            assertEquals(2, tracker.deleteMyAccountCallCount)
        }

    @Test
    fun `SET-BR-021 - a successful account erasure delegates once and returns success`() =
        runTest {
            val tracker = FakeTrackerAccountRepository(deleteMyAccountResult = Result.success(Unit))
            val vm = newVm(tracker = tracker)

            val result = vm.deleteMyAccount()

            assertTrue(result.isSuccess)
            assertEquals(1, tracker.deleteMyAccountCallCount)
        }

    // NOTE: `SET-BR-023` ("no export row while no records exist") has no test here either, and
    // closes **deferred** in the QA catalog rather than pretending otherwise. It was tested by
    // reflecting over this class's member names for the substring "export" — which asserts that
    // the author didn't pick a particular identifier, not that the app behaves correctly. The row
    // is removed outright (T053, research R7), so there is no conditional to exercise; the real
    // assertion becomes possible only when the phase that reinstates the row ships the records
    // repository it gates on.
}
