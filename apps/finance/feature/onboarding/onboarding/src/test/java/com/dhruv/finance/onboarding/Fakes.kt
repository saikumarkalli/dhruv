package com.dhruv.finance.onboarding

import com.dhruv.finance.data.tracker.auth.AuthRepository
import com.dhruv.finance.data.tracker.auth.ConsentRepository
import com.dhruv.finance.data.tracker.auth.ConsentState
import com.dhruv.finance.data.tracker.auth.SessionState
import com.dhruv.finance.data.tracker.auth.SessionStore
import com.dhruv.finance.data.tracker.auth.SessionTokens
import com.dhruv.finance.data.tracker.dto.GoTrueSessionDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Local hand-written fakes for [OnboardingViewModelTest]. `:apps:finance:data`'s own Fakes.kt
 * (`FakeSessionStore`/`FakeConsentRepository`) lives in that module's **test** sourceset, which is
 * not visible from this module's tests — this repo has no `testFixtures` wiring for cross-module
 * test-code sharing (confirmed against every existing feature module's build.gradle.kts). Fakes,
 * never mocks, per the project convention — duplicating ~15 lines each here is the accepted cost.
 */

class FakeSessionStore(
    initial: SessionState = SessionState.SignedOut,
) : SessionStore {
    private val _state = MutableStateFlow(initial)
    override val state: StateFlow<SessionState> = _state.asStateFlow()

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
        tokens = null
        _state.value = SessionState.SignedOut
    }

    override fun currentTokens(): SessionTokens? = tokens
}

class FakeConsentRepository : ConsentRepository {
    private val _state = MutableStateFlow(ConsentState())
    override val state: StateFlow<ConsentState> = _state.asStateFlow()

    var setSyncFinancialRecordsCallCount = 0
        private set
    var setReadTransactionSmsCallCount = 0
        private set
    var setAskDhruvAboutMoneyCallCount = 0
        private set
    var setHasCompletedOnboardingCallCount = 0
        private set

    val totalSetterCallCount: Int
        get() = setSyncFinancialRecordsCallCount + setReadTransactionSmsCallCount + setAskDhruvAboutMoneyCallCount

    override suspend fun setSyncFinancialRecords(enabled: Boolean) {
        setSyncFinancialRecordsCallCount++
        _state.value = _state.value.copy(syncFinancialRecords = enabled)
    }

    override suspend fun setReadTransactionSms(enabled: Boolean) {
        setReadTransactionSmsCallCount++
        _state.value = _state.value.copy(readTransactionSms = enabled)
    }

    override suspend fun setAskDhruvAboutMoney(enabled: Boolean) {
        setAskDhruvAboutMoneyCallCount++
        _state.value = _state.value.copy(askDhruvAboutMoney = enabled)
    }

    override suspend fun setHasCompletedOnboarding(completed: Boolean) {
        setHasCompletedOnboardingCallCount++
        _state.value = _state.value.copy(hasCompletedOnboarding = completed)
    }
}

class FakeAuthRepository(
    private val result: Result<Unit> = Result.success(Unit),
) : AuthRepository {
    var callCount = 0
        private set
    var lastIdToken: String? = null
        private set
    var lastRawNonce: String? = null
        private set

    override suspend fun signInWithGoogleIdToken(
        idToken: String,
        rawNonce: String,
    ): Result<Unit> {
        callCount++
        lastIdToken = idToken
        lastRawNonce = rawNonce
        return result
    }
}
