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
import com.dhruv.finance.data.tracker.model.CreateLiabilityMetaRequest
import com.dhruv.finance.data.tracker.model.Holding
import com.dhruv.finance.data.tracker.model.HoldingKind
import com.dhruv.finance.data.tracker.model.HoldingWithValue
import com.dhruv.finance.data.tracker.model.LiabilityMeta
import com.dhruv.finance.data.tracker.model.LiabilityType
import com.dhruv.finance.data.tracker.model.Sector
import com.dhruv.finance.data.tracker.model.UpdateHoldingRequest
import com.dhruv.finance.data.tracker.model.UpdateLiabilityMetaRequest
import com.dhruv.finance.data.tracker.repo.HoldingRepository
import com.dhruv.finance.data.tracker.repo.LiabilityRepository
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

private class FakeLiabilitiesHoldingRepository(
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

private class FakeLiabilitiesLiabilityRepository(
    private val listAllResult: Result<List<LiabilityMeta>> = Result.success(emptyList()),
) : LiabilityRepository {
    override suspend fun createMeta(request: CreateLiabilityMetaRequest): Result<Unit> =
        throw UnsupportedOperationException("not exercised by this test")

    override suspend fun listAll(): Result<List<LiabilityMeta>> = listAllResult

    override suspend fun get(holdingId: String): Result<LiabilityMeta> = throw UnsupportedOperationException("not exercised by this test")

    override suspend fun updateMeta(
        holdingId: String,
        request: UpdateLiabilityMetaRequest,
    ): Result<Unit> = throw UnsupportedOperationException("not exercised by this test")
}

private class FakeLiabilitiesSessionStore : SessionStore {
    override val state: StateFlow<SessionState> = MutableStateFlow(SessionState.Active("u1", "a@b.com", null, null))

    override suspend fun save(session: GoTrueSessionDto) = Unit

    override suspend fun clear() = Unit

    override fun currentTokens(): SessionTokens? = null
}

private class FakeLiabilitiesConsentRepository : ConsentRepository {
    override val state: StateFlow<ConsentState> = MutableStateFlow(ConsentState(syncFinancialRecords = true))

    override suspend fun setSyncFinancialRecords(enabled: Boolean) = Unit

    override suspend fun setReadTransactionSms(enabled: Boolean) = Unit

    override suspend fun setAskDhruvAboutMoney(enabled: Boolean) = Unit

    override suspend fun setHasCompletedOnboarding(completed: Boolean) = Unit
}

private fun liabilityHolding(
    id: String,
    name: String,
    valuePaise: Long,
) = HoldingWithValue(
    holding = Holding(id, name, HoldingKind.LIABILITY, Sector.OTHER, null, null),
    currentValuePaise = valuePaise,
)

@OptIn(ExperimentalCoroutinesApi::class)
class LiabilitiesViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // spec.md Story 4 Scenario 1: grouped by type, with outstanding/outgo totals and a projected
    // debt-free date.
    @Test
    fun `load merges holdings with their liability terms and groups by type`() =
        runTest(dispatcher) {
            val holdings =
                listOf(
                    liabilityHolding("h1", "Home Loan", 47_50_000_00L),
                    liabilityHolding("h2", "Credit Card", 25_000_00L),
                )
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
            val vm =
                LiabilitiesViewModel(
                    holdingRepository = FakeLiabilitiesHoldingRepository(Result.success(holdings)),
                    liabilityRepository = FakeLiabilitiesLiabilityRepository(Result.success(metas)),
                    sessionStore = FakeLiabilitiesSessionStore(),
                    consentRepository = FakeLiabilitiesConsentRepository(),
                    crashReporter = NoOpCrashReporter,
                    performanceTracer = NoOpPerformanceTracer,
                )
            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals(2, state.rows.size)
            val grouped = vm.groupedByType(state)
            assertEquals(1, grouped[LiabilityType.HOME_LOAN]?.size)
            assertEquals(1, grouped[LiabilityType.CREDIT_CARD]?.size)
            assertEquals(47_75_000_00L, vm.totalOutstandingPaise(state))
            assertEquals(45_000_00L, vm.monthlyOutgoPaise(state))
        }

    @Test
    fun `payoffProgress is the paid-months fraction of tenure, null without a known tenure`() =
        runTest(dispatcher) {
            val withTenure =
                LiabilityRow(
                    holdingWithValue = liabilityHolding("h1", "Home Loan", 47_50_000_00L),
                    meta =
                        LiabilityMeta(
                            holdingId = "h1",
                            liabilityType = LiabilityType.HOME_LOAN,
                            rateBps = 850,
                            emiPaise = 45_000_00L,
                            debitDay = 5,
                            tenureMonths = 240,
                            paidMonths = 60,
                            originalPrincipalPaise = 50_00_000_00L,
                            collateral = null,
                            linkedAccountId = null,
                        ),
                )
            val withoutTenure =
                LiabilityRow(
                    holdingWithValue = liabilityHolding("h2", "Credit Card", 25_000_00L),
                    meta =
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
            val vm =
                LiabilitiesViewModel(
                    holdingRepository = FakeLiabilitiesHoldingRepository(),
                    liabilityRepository = FakeLiabilitiesLiabilityRepository(),
                    sessionStore = FakeLiabilitiesSessionStore(),
                    consentRepository = FakeLiabilitiesConsentRepository(),
                    crashReporter = NoOpCrashReporter,
                    performanceTracer = NoOpPerformanceTracer,
                )
            advanceUntilIdle()

            assertEquals(0.25f, vm.payoffProgress(withTenure))
            assertNull(vm.payoffProgress(withoutTenure))
        }

    @Test
    fun `debtFreeBy is null when no row has enough terms to project`() =
        runTest(dispatcher) {
            val holdings = listOf(liabilityHolding("h1", "Credit Card", 25_000_00L))
            val metas =
                listOf(
                    LiabilityMeta(
                        holdingId = "h1",
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
            val vm =
                LiabilitiesViewModel(
                    holdingRepository = FakeLiabilitiesHoldingRepository(Result.success(holdings)),
                    liabilityRepository = FakeLiabilitiesLiabilityRepository(Result.success(metas)),
                    sessionStore = FakeLiabilitiesSessionStore(),
                    consentRepository = FakeLiabilitiesConsentRepository(),
                    crashReporter = NoOpCrashReporter,
                    performanceTracer = NoOpPerformanceTracer,
                )
            advanceUntilIdle()

            assertNull(vm.debtFreeBy(vm.uiState.value))
        }
}
