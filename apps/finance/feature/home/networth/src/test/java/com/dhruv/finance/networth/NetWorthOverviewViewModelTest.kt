package com.dhruv.finance.networth

import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.core.observability.NoOpPerformanceTracer
import com.dhruv.finance.data.tracker.auth.ConsentRepository
import com.dhruv.finance.data.tracker.auth.ConsentState
import com.dhruv.finance.data.tracker.auth.SessionState
import com.dhruv.finance.data.tracker.auth.SessionStore
import com.dhruv.finance.data.tracker.auth.SessionTokens
import com.dhruv.finance.data.tracker.dto.GoTrueSessionDto
import com.dhruv.finance.data.tracker.model.HoldingKind
import com.dhruv.finance.data.tracker.model.NetWorthSummary
import com.dhruv.finance.data.tracker.model.Sector
import com.dhruv.finance.data.tracker.model.SectorBreakdown
import com.dhruv.finance.data.tracker.repo.NetWorthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

private class FakeNetWorthRepository(
    private val result: Result<NetWorthSummary>,
) : NetWorthRepository {
    override suspend fun getSummary(): Result<NetWorthSummary> = result
}

private class FakeSessionStore : SessionStore {
    override val state: StateFlow<SessionState> = MutableStateFlow(SessionState.SignedOut)

    override suspend fun save(session: GoTrueSessionDto) = Unit

    override suspend fun clear() = Unit

    override fun currentTokens(): SessionTokens? = null
}

private class FakeConsentRepository : ConsentRepository {
    override val state: StateFlow<ConsentState> = MutableStateFlow(ConsentState())

    override suspend fun setSyncFinancialRecords(enabled: Boolean) = Unit

    override suspend fun setReadTransactionSms(enabled: Boolean) = Unit

    override suspend fun setAskDhruvAboutMoney(enabled: Boolean) = Unit

    override suspend fun setHasCompletedOnboarding(completed: Boolean) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class NetWorthOverviewViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // NW-UI-001: a successful load surfaces the summary and clears loading/error state.
    @Test
    fun `load success populates the summary`() =
        runTest(dispatcher) {
            val summary =
                NetWorthSummary(
                    netPaise = 4_00_000_00L,
                    assetsPaise = 6_00_000_00L,
                    liabilitiesPaise = 2_00_000_00L,
                    bySector = listOf(SectorBreakdown(HoldingKind.ASSET, Sector.BANK, 1, 6_00_000_00L)),
                )
            val vm =
                NetWorthOverviewViewModel(
                    netWorthRepository = FakeNetWorthRepository(Result.success(summary)),
                    sessionStore = FakeSessionStore(),
                    consentRepository = FakeConsentRepository(),
                    crashReporter = NoOpCrashReporter,
                    performanceTracer = NoOpPerformanceTracer,
                )
            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals(false, state.isLoading)
            assertEquals(summary, state.summary)
            assertNull(state.errorMessage)
        }

    // A failed load surfaces an error message rather than leaving the screen stuck loading.
    @Test
    fun `load failure surfaces an error message`() =
        runTest(dispatcher) {
            val vm =
                NetWorthOverviewViewModel(
                    netWorthRepository = FakeNetWorthRepository(Result.failure(RuntimeException("boom"))),
                    sessionStore = FakeSessionStore(),
                    consentRepository = FakeConsentRepository(),
                    crashReporter = NoOpCrashReporter,
                    performanceTracer = NoOpPerformanceTracer,
                )
            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals(false, state.isLoading)
            assertEquals("boom", state.errorMessage)
            assertNull(state.summary)
        }
}
