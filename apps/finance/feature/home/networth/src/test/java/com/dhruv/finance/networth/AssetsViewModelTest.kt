package com.dhruv.finance.networth

import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.core.observability.NoOpPerformanceTracer
import com.dhruv.finance.data.tracker.auth.ConsentRepository
import com.dhruv.finance.data.tracker.auth.ConsentState
import com.dhruv.finance.data.tracker.auth.SessionState
import com.dhruv.finance.data.tracker.auth.SessionStore
import com.dhruv.finance.data.tracker.auth.SessionTokens
import com.dhruv.finance.data.tracker.dto.GoTrueSessionDto
import com.dhruv.finance.data.tracker.model.CreateHoldingRequest
import com.dhruv.finance.data.tracker.model.Holding
import com.dhruv.finance.data.tracker.model.HoldingKind
import com.dhruv.finance.data.tracker.model.HoldingWithValue
import com.dhruv.finance.data.tracker.model.Sector
import com.dhruv.finance.data.tracker.model.UpdateHoldingRequest
import com.dhruv.finance.data.tracker.repo.HoldingRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeAssetsHoldingRepository(
    private val listResult: Result<List<HoldingWithValue>> = Result.success(emptyList()),
) : HoldingRepository {
    override suspend fun createWithFirstValuation(request: CreateHoldingRequest): Result<String> =
        throw UnsupportedOperationException("not exercised by this test")

    override suspend fun list(kind: HoldingKind): Result<List<HoldingWithValue>> = listResult

    override suspend fun get(holdingId: String): Result<Holding> = throw UnsupportedOperationException("not exercised by this test")

    override suspend fun update(
        holdingId: String,
        request: UpdateHoldingRequest,
    ): Result<Unit> = throw UnsupportedOperationException("not exercised by this test")

    override suspend fun softDelete(holdingId: String): Result<Unit> = throw UnsupportedOperationException("not exercised by this test")

    override suspend fun restore(holdingId: String): Result<Unit> = throw UnsupportedOperationException("not exercised by this test")
}

private class FakeAssetsSessionStore : SessionStore {
    override val state: StateFlow<SessionState> = MutableStateFlow(SessionState.Active("u1", "a@b.com", null, null))

    override suspend fun save(session: GoTrueSessionDto) = Unit

    override suspend fun clear() = Unit

    override fun currentTokens(): SessionTokens? = null
}

private class FakeAssetsConsentRepository : ConsentRepository {
    override val state: StateFlow<ConsentState> = MutableStateFlow(ConsentState(syncFinancialRecords = true))

    override suspend fun setSyncFinancialRecords(enabled: Boolean) = Unit

    override suspend fun setReadTransactionSms(enabled: Boolean) = Unit

    override suspend fun setAskDhruvAboutMoney(enabled: Boolean) = Unit

    override suspend fun setHasCompletedOnboarding(completed: Boolean) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class AssetsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load populates holdings from the repository`() =
        runTest(dispatcher) {
            val holding =
                HoldingWithValue(
                    holding = Holding("id-1", "HDFC Savings", HoldingKind.ASSET, Sector.BANK, null, null),
                    currentValuePaise = 50_000_00L,
                )
            val vm =
                AssetsViewModel(
                    holdingRepository = FakeAssetsHoldingRepository(Result.success(listOf(holding))),
                    sessionStore = FakeAssetsSessionStore(),
                    consentRepository = FakeAssetsConsentRepository(),
                    crashReporter = NoOpCrashReporter,
                    performanceTracer = NoOpPerformanceTracer,
                )
            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals(false, state.isLoading)
            assertEquals(1, state.holdings.size)
            assertEquals(
                "HDFC Savings",
                state.holdings
                    .first()
                    .holding.name,
            )
        }

    @Test
    fun `setSectorFilter updates the selected filter without reloading`() =
        runTest(dispatcher) {
            val vm =
                AssetsViewModel(
                    holdingRepository = FakeAssetsHoldingRepository(),
                    sessionStore = FakeAssetsSessionStore(),
                    consentRepository = FakeAssetsConsentRepository(),
                    crashReporter = NoOpCrashReporter,
                    performanceTracer = NoOpPerformanceTracer,
                )
            advanceUntilIdle()

            vm.setSectorFilter(Sector.GOLD)

            assertTrue(vm.uiState.value.selectedSectorFilter == Sector.GOLD)
        }
}
