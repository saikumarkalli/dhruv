package com.dhruv.finance.app.ui.home

import com.dhruv.core.navigation.TabKey
import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.core.observability.NoOpPerformanceTracer
import com.dhruv.finance.data.tracker.auth.ConsentRepository
import com.dhruv.finance.data.tracker.auth.ConsentState
import com.dhruv.finance.data.tracker.auth.SessionState
import com.dhruv.finance.data.tracker.auth.SessionStore
import com.dhruv.finance.data.tracker.model.CreateHoldingRequest
import com.dhruv.finance.data.tracker.model.CreateLiabilityMetaRequest
import com.dhruv.finance.data.tracker.model.Holding
import com.dhruv.finance.data.tracker.model.HoldingKind
import com.dhruv.finance.data.tracker.model.HoldingWithValue
import com.dhruv.finance.data.tracker.model.LiabilityMeta
import com.dhruv.finance.data.tracker.model.LiabilityType
import com.dhruv.finance.data.tracker.model.NetWorthHistoryPoint
import com.dhruv.finance.data.tracker.model.NetWorthSummary
import com.dhruv.finance.data.tracker.model.Sector
import com.dhruv.finance.data.tracker.model.UpdateHoldingRequest
import com.dhruv.finance.data.tracker.model.UpdateLiabilityMetaRequest
import com.dhruv.finance.data.tracker.repo.HoldingRepository
import com.dhruv.finance.data.tracker.repo.LiabilityRepository
import com.dhruv.finance.data.tracker.repo.NetWorthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

private class FakeHomeNetWorthRepository(
    private val historyResult: Result<List<NetWorthHistoryPoint>> = Result.success(emptyList()),
) : NetWorthRepository {
    override suspend fun getSummary(): Result<NetWorthSummary> = throw UnsupportedOperationException("not exercised by this test")

    override suspend fun getHistory(): Result<List<NetWorthHistoryPoint>> = historyResult
}

private class FakeHomeHoldingRepository(
    private val liabilities: List<HoldingWithValue> = emptyList(),
) : HoldingRepository {
    override suspend fun createWithFirstValuation(request: CreateHoldingRequest): Result<String> =
        throw UnsupportedOperationException("not exercised by this test")

    override suspend fun list(kind: HoldingKind): Result<List<HoldingWithValue>> = Result.success(liabilities)

    override suspend fun get(holdingId: String): Result<Holding> = throw UnsupportedOperationException("not exercised by this test")

    override suspend fun update(
        holdingId: String,
        request: UpdateHoldingRequest,
    ): Result<Unit> = throw UnsupportedOperationException("not exercised by this test")

    override suspend fun softDelete(holdingId: String): Result<Unit> = throw UnsupportedOperationException("not exercised by this test")

    override suspend fun restore(holdingId: String): Result<Unit> = throw UnsupportedOperationException("not exercised by this test")
}

private class FakeHomeLiabilityRepository(
    private val metas: List<LiabilityMeta> = emptyList(),
) : LiabilityRepository {
    override suspend fun createMeta(request: CreateLiabilityMetaRequest): Result<Unit> =
        throw UnsupportedOperationException("not exercised by this test")

    override suspend fun listAll(): Result<List<LiabilityMeta>> = Result.success(metas)

    override suspend fun get(holdingId: String): Result<LiabilityMeta> = throw UnsupportedOperationException("not exercised by this test")

    override suspend fun updateMeta(
        holdingId: String,
        request: UpdateLiabilityMetaRequest,
    ): Result<Unit> = throw UnsupportedOperationException("not exercised by this test")
}

private class FakeHomeSessionStore(
    initial: SessionState = SessionState.Active("u1", "a@b.com", null, null),
) : SessionStore {
    private val _state = MutableStateFlow(initial)
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    override suspend fun save(session: com.dhruv.finance.data.tracker.dto.GoTrueSessionDto) = Unit

    override suspend fun clear() = Unit

    override fun currentTokens(): com.dhruv.finance.data.tracker.auth.SessionTokens? = null
}

private class FakeHomeConsentRepository(
    initial: ConsentState = ConsentState(syncFinancialRecords = true),
) : ConsentRepository {
    private val _state = MutableStateFlow(initial)
    override val state: StateFlow<ConsentState> = _state.asStateFlow()

    override suspend fun setSyncFinancialRecords(enabled: Boolean) {
        _state.value = _state.value.copy(syncFinancialRecords = enabled)
    }

    override suspend fun setReadTransactionSms(enabled: Boolean) = Unit

    override suspend fun setAskDhruvAboutMoney(enabled: Boolean) = Unit

    override suspend fun setHasCompletedOnboarding(completed: Boolean) = Unit
}

private fun liabilityHolding(
    id: String,
    name: String,
) = HoldingWithValue(
    holding = Holding(id, name, HoldingKind.LIABILITY, Sector.OTHER, null, null),
    currentValuePaise = 25_000_00L,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        history: List<NetWorthHistoryPoint> = emptyList(),
        liabilityHoldings: List<HoldingWithValue> = emptyList(),
        liabilityMetas: List<LiabilityMeta> = emptyList(),
        sessionStore: SessionStore = FakeHomeSessionStore(),
        consentRepository: ConsentRepository = FakeHomeConsentRepository(),
    ) = HomeViewModel(
        netWorthRepository = FakeHomeNetWorthRepository(Result.success(history)),
        holdingRepository = FakeHomeHoldingRepository(liabilityHoldings),
        liabilityRepository = FakeHomeLiabilityRepository(liabilityMetas),
        sessionStore = sessionStore,
        consentRepository = consentRepository,
        crashReporter = NoOpCrashReporter,
        performanceTracer = NoOpPerformanceTracer,
    )

    // HOM-UI-001: the hero figure is C1's own total — the newest v_net_worth_history point.
    @Test
    fun `hero figure matches the latest history point`() =
        runTest(dispatcher) {
            val history =
                listOf(
                    NetWorthHistoryPoint("2026-08-01", 4_00_000_00L),
                    NetWorthHistoryPoint("2026-08-31", 4_25_000_00L),
                )
            val vm = viewModel(history = history)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals(false, state.isLoading)
            assertEquals(4_25_000_00L, state.netPaise)
            assertEquals(625, state.deltaPercentBps) // +25,000.00 / 4,00,000.00 = 6.25% = 625 bps
        }

    @Test
    fun `load surfaces an error when history can't be loaded`() =
        runTest(dispatcher) {
            val vm =
                HomeViewModel(
                    netWorthRepository = FakeHomeNetWorthRepository(Result.failure(java.io.IOException("down"))),
                    holdingRepository = FakeHomeHoldingRepository(),
                    liabilityRepository = FakeHomeLiabilityRepository(),
                    sessionStore = FakeHomeSessionStore(),
                    consentRepository = FakeHomeConsentRepository(),
                    crashReporter = NoOpCrashReporter,
                    performanceTracer = NoOpPerformanceTracer,
                )
            advanceUntilIdle()

            assertNull(vm.uiState.value.netPaise)
            assertTrue(vm.uiState.value.errorMessage != null)
        }

    // HOM-UI-003: UPCOMING is EMI-only this phase — a liability needs both an EMI and a debit day
    // to produce a row; card-bill rows (no debit_day-driven EMI schedule) wait for Phase 3.
    @Test
    fun `UPCOMING only includes liabilities with both an EMI and a debit day`() =
        runTest(dispatcher) {
            val holdings = listOf(liabilityHolding("h1", "Home Loan"), liabilityHolding("h2", "Credit Card"))
            val metas =
                listOf(
                    LiabilityMeta(
                        holdingId = "h1",
                        liabilityType = LiabilityType.HOME_LOAN,
                        rateBps = 850,
                        emiPaise = 45_000_00L,
                        debitDay = 5,
                        tenureMonths = 240,
                        paidMonths = 24,
                        originalPrincipalPaise = 50_00_000_00L,
                        collateral = null,
                        linkedAccountId = null,
                    ),
                    LiabilityMeta(
                        holdingId = "h2",
                        liabilityType = LiabilityType.CREDIT_CARD,
                        rateBps = 3600,
                        emiPaise = null,
                        debitDay = 10,
                        tenureMonths = null,
                        paidMonths = 0,
                        originalPrincipalPaise = null,
                        collateral = null,
                        linkedAccountId = null,
                    ),
                )
            val vm = viewModel(liabilityHoldings = holdings, liabilityMetas = metas)
            advanceUntilIdle()

            val upcoming = vm.uiState.value.upcoming
            assertEquals(1, upcoming.size)
            assertEquals("Home Loan", upcoming.first().name)
        }

    @Test
    fun `deltaBps compares the two newest history points`() {
        val history = listOf(NetWorthHistoryPoint("2026-08-01", 4_00_000_00L), NetWorthHistoryPoint("2026-08-31", 4_50_000_00L))

        assertEquals(1250, deltaBps(history)) // +50,000.00 / 4,00,000.00 = 12.5%
    }

    @Test
    fun `deltaBps is null with fewer than two points`() {
        assertNull(deltaBps(listOf(NetWorthHistoryPoint("2026-08-01", 4_00_000_00L))))
        assertNull(deltaBps(emptyList()))
    }

    @Test
    fun `nextDueDate stays in the current month when the debit day hasn't passed`() {
        val today = LocalDate.of(2026, 9, 2)

        assertEquals(LocalDate.of(2026, 9, 5), nextDueDate(debitDay = 5, today = today))
    }

    @Test
    fun `nextDueDate rolls to next month when the debit day already passed`() {
        val today = LocalDate.of(2026, 9, 10)

        assertEquals(LocalDate.of(2026, 10, 5), nextDueDate(debitDay = 5, today = today))
    }

    @Test
    fun `nextDueDate clamps a debit day beyond the month's length`() {
        val today = LocalDate.of(2026, 2, 20) // February, 28 days in 2026

        assertEquals(LocalDate.of(2026, 2, 28), nextDueDate(debitDay = 31, today = today))
    }

    @Test
    fun `greetingForHour matches time of day`() {
        assertEquals("Good Morning", greetingForHour(8))
        assertEquals("Good Afternoon", greetingForHour(14))
        assertEquals("Good Evening", greetingForHour(19))
        assertEquals("Good Night", greetingForHour(2))
    }

    @Test
    fun `firstNameFrom takes only the first token of a full display name`() {
        assertEquals("Sai", firstNameFrom("Sai Kumar"))
        assertEquals("Sai", firstNameFrom("Sai"))
    }

    @Test
    fun `firstNameFrom is null when no name is available`() {
        assertEquals(null, firstNameFrom(null))
        assertEquals(null, firstNameFrom("   "))
    }

    // HOM-UI-004/ADR-0024 decision 4: the Ask pill renders on Home/Plan/Insights, not Calc/Money.
    @Test
    fun `shouldShowAskPill renders on Home, Plan and Insights but not Calc or Money`() {
        assertTrue(shouldShowAskPill(TabKey.HOME))
        assertTrue(shouldShowAskPill(TabKey.PLAN))
        assertTrue(shouldShowAskPill(TabKey.INSIGHTS))
        assertFalse(shouldShowAskPill(TabKey.CALC))
        assertFalse(shouldShowAskPill(TabKey.MONEY))
    }
}
