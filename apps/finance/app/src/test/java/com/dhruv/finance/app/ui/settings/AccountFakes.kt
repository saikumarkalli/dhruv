package com.dhruv.finance.app.ui.settings

import com.dhruv.finance.data.tracker.auth.AuthRepository
import com.dhruv.finance.data.tracker.auth.ConsentRepository
import com.dhruv.finance.data.tracker.auth.ConsentState
import com.dhruv.finance.data.tracker.auth.SessionState
import com.dhruv.finance.data.tracker.auth.SessionStore
import com.dhruv.finance.data.tracker.auth.SessionTokens
import com.dhruv.finance.data.tracker.auth.TrackerAccountRepository
import com.dhruv.finance.data.tracker.dto.GoTrueSessionDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Local hand-written fakes for [AccountSettingsViewModelTest]. `:apps:finance:data`'s and
 * `:onboarding`'s own equivalents live in those modules' **test** sourcesets, not visible here —
 * this repo has no `testFixtures` wiring for cross-module test-code sharing (same note as
 * onboarding's own `Fakes.kt`). Fakes, never mocks, per project convention.
 */

class FakeSessionStoreForAccount(
    initial: SessionState = SessionState.SignedOut,
) : SessionStore {
    private val _state = MutableStateFlow(initial)
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    var clearCallCount = 0
        private set
    private var tokens: SessionTokens? = null

    override suspend fun save(session: GoTrueSessionDto) {
        tokens = SessionTokens(session.accessToken, session.refreshToken, session.expiresAt)
        _state.value =
            SessionState.Active(
                session.user.id,
                session.user.email,
                session.user.userMetadata?.displayName,
                session.user.userMetadata?.resolvedAvatarUrl,
            )
    }

    override suspend fun clear() {
        clearCallCount++
        tokens = null
        _state.value = SessionState.SignedOut
    }

    override fun currentTokens(): SessionTokens? = tokens
}

class FakeConsentRepositoryForAccount : ConsentRepository {
    private val _state = MutableStateFlow(ConsentState())
    override val state: StateFlow<ConsentState> = _state.asStateFlow()

    override suspend fun setSyncFinancialRecords(enabled: Boolean) {
        _state.value = _state.value.copy(syncFinancialRecords = enabled)
    }

    override suspend fun setReadTransactionSms(enabled: Boolean) {
        _state.value = _state.value.copy(readTransactionSms = enabled)
    }

    override suspend fun setAskDhruvAboutMoney(enabled: Boolean) {
        _state.value = _state.value.copy(askDhruvAboutMoney = enabled)
    }

    override suspend fun setHasCompletedOnboarding(completed: Boolean) {
        _state.value = _state.value.copy(hasCompletedOnboarding = completed)
    }
}

class FakeAuthRepositoryForAccount(
    private val result: Result<Unit> = Result.success(Unit),
) : AuthRepository {
    var callCount = 0
        private set

    override suspend fun signInWithGoogleIdToken(
        idToken: String,
        rawNonce: String,
    ): Result<Unit> {
        callCount++
        return result
    }
}

class FakeTrackerAccountRepository(
    private val deleteMyDataResult: Result<Unit> = Result.success(Unit),
    private val deleteMyAccountResult: Result<Unit> = Result.success(Unit),
) : TrackerAccountRepository {
    var deleteMyDataCallCount = 0
        private set
    var deleteMyAccountCallCount = 0
        private set

    override suspend fun deleteMyData(): Result<Unit> {
        deleteMyDataCallCount++
        return deleteMyDataResult
    }

    override suspend fun deleteMyAccount(): Result<Unit> {
        deleteMyAccountCallCount++
        return deleteMyAccountResult
    }
}
