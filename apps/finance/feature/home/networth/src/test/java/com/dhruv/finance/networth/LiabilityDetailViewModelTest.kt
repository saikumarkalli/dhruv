package com.dhruv.finance.networth

import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.core.observability.NoOpPerformanceTracer
import com.dhruv.finance.data.tracker.model.CreateHoldingRequest
import com.dhruv.finance.data.tracker.model.CreateLiabilityMetaRequest
import com.dhruv.finance.data.tracker.model.Holding
import com.dhruv.finance.data.tracker.model.HoldingKind
import com.dhruv.finance.data.tracker.model.HoldingWithValue
import com.dhruv.finance.data.tracker.model.LiabilityMeta
import com.dhruv.finance.data.tracker.model.LiabilityType
import com.dhruv.finance.data.tracker.model.Sector
import com.dhruv.finance.data.tracker.model.UpdateLiabilityMetaRequest
import com.dhruv.finance.data.tracker.model.Valuation
import com.dhruv.finance.data.tracker.model.ValuationHistoryEntry
import com.dhruv.finance.data.tracker.model.ValuationSource
import com.dhruv.finance.data.tracker.repo.HoldingRepository
import com.dhruv.finance.data.tracker.repo.LiabilityRepository
import com.dhruv.finance.data.tracker.repo.ValuationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

private class FakeLiabilityDetailHoldingRepository(
    private val result: Result<Holding>,
) : HoldingRepository {
    override suspend fun createWithFirstValuation(request: CreateHoldingRequest): Result<String> =
        throw UnsupportedOperationException("not exercised by this test")

    override suspend fun list(kind: HoldingKind): Result<List<HoldingWithValue>> =
        throw UnsupportedOperationException("not exercised by this test")

    override suspend fun get(holdingId: String): Result<Holding> = result
}

private class FakeLiabilityDetailLiabilityRepository(
    private val result: Result<LiabilityMeta> = Result.failure(NoSuchElementException("no meta")),
) : LiabilityRepository {
    override suspend fun createMeta(request: CreateLiabilityMetaRequest): Result<Unit> =
        throw UnsupportedOperationException("not exercised by this test")

    override suspend fun listAll(): Result<List<LiabilityMeta>> = throw UnsupportedOperationException("not exercised by this test")

    override suspend fun get(holdingId: String): Result<LiabilityMeta> = result

    override suspend fun updateMeta(
        holdingId: String,
        request: UpdateLiabilityMetaRequest,
    ): Result<Unit> = throw UnsupportedOperationException("not exercised by this test")
}

private class FakeLiabilityDetailValuationRepository(
    private val history: Result<List<ValuationHistoryEntry>> = Result.success(emptyList()),
) : ValuationRepository {
    override suspend fun listHistory(holdingId: String): Result<List<ValuationHistoryEntry>> = history

    override suspend fun recordValue(
        holdingId: String,
        valuePaise: Long,
        asOf: String,
        sourceCode: String,
        requestId: String?,
    ): Result<String> = throw UnsupportedOperationException("not exercised by this test")

    override suspend fun correctValue(
        valuationId: String,
        valuePaise: Long,
        asOf: String,
        note: String?,
    ): Result<String> = throw UnsupportedOperationException("not exercised by this test")
}

private fun loanMeta(paidMonths: Int = 24) =
    LiabilityMeta(
        holdingId = "h1",
        liabilityType = LiabilityType.HOME_LOAN,
        rateBps = 850,
        emiPaise = 45_000_00L,
        debitDay = 5,
        tenureMonths = 240,
        paidMonths = paidMonths,
        originalPrincipalPaise = 50_00_000_00L,
        collateral = "Flat, Bengaluru",
        linkedAccountId = null,
    )

private fun outstandingHistory(valuePaise: Long) =
    listOf(ValuationHistoryEntry(Valuation("v1", "h1", valuePaise, "2026-08-01", ValuationSource.MANUAL), null, null))

@OptIn(ExperimentalCoroutinesApi::class)
class LiabilityDetailViewModelTest {
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
        holdingResult: Result<Holding>,
        liabilityResult: Result<LiabilityMeta> = Result.failure(NoSuchElementException("no meta")),
        historyResult: Result<List<ValuationHistoryEntry>> = Result.success(emptyList()),
    ) = LiabilityDetailViewModel(
        holdingRepository = FakeLiabilityDetailHoldingRepository(holdingResult),
        liabilityRepository = FakeLiabilityDetailLiabilityRepository(liabilityResult),
        valuationRepository = FakeLiabilityDetailValuationRepository(historyResult),
        crashReporter = NoOpCrashReporter,
        performanceTracer = NoOpPerformanceTracer,
    )

    @Test
    fun `load populates the holding, meta, and outstanding balance`() =
        runTest(dispatcher) {
            val holding = Holding("h1", "HDFC Home Loan", HoldingKind.LIABILITY, Sector.PROPERTY, null, null)
            val vm = viewModel(Result.success(holding), Result.success(loanMeta()), Result.success(outstandingHistory(47_50_000_00L)))

            vm.load("h1")
            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals(false, state.isLoading)
            assertEquals("HDFC Home Loan", state.holding?.name)
            assertEquals(LiabilityType.HOME_LOAN, state.meta?.liabilityType)
            assertEquals(47_50_000_00L, state.outstandingPaise)
            assertNull(state.errorMessage)
        }

    // A missing liabilities_meta row is a designed non-blocking state, not a load failure.
    @Test
    fun `load succeeds with a null meta when no liabilities_meta row exists`() =
        runTest(dispatcher) {
            val holding = Holding("h1", "Old Loan", HoldingKind.LIABILITY, Sector.OTHER, null, null)
            val vm = viewModel(Result.success(holding))

            vm.load("h1")
            advanceUntilIdle()

            val state = vm.uiState.value
            assertNull(state.errorMessage)
            assertNull(state.meta)
        }

    @Test
    fun `load failure surfaces an error message when the holding itself can't be found`() =
        runTest(dispatcher) {
            val vm = viewModel(Result.failure(NoSuchElementException("gone")))

            vm.load("missing")
            advanceUntilIdle()

            assertNotNull(vm.uiState.value.errorMessage)
        }

    // spec.md Story 4 Scenario 2: the three parts sum to the total obligation.
    @Test
    fun `amortisationSplit reflects the loaded meta and outstanding balance`() =
        runTest(dispatcher) {
            val holding = Holding("h1", "HDFC Home Loan", HoldingKind.LIABILITY, Sector.PROPERTY, null, null)
            val vm = viewModel(Result.success(holding), Result.success(loanMeta()), Result.success(outstandingHistory(47_50_000_00L)))
            vm.load("h1")
            advanceUntilIdle()

            val split = vm.amortisationSplit(vm.uiState.value)!!

            assertEquals(47_50_000_00L, split.remainingPaise)
            assertEquals(2_50_000_00L, split.principalPaidPaise)
        }

    @Test
    fun `computePrepay reports an error without loan terms`() =
        runTest(dispatcher) {
            val holding = Holding("h1", "Old Loan", HoldingKind.LIABILITY, Sector.OTHER, null, null)
            val vm = viewModel(Result.success(holding))
            vm.load("h1")
            advanceUntilIdle()
            vm.onExtraPaymentChange("10000")

            vm.computePrepay()

            assertNotNull(vm.uiState.value.prepayError)
            assertNull(vm.uiState.value.prepayProjection)
        }

    @Test
    fun `computePrepay rejects a non-numeric amount`() =
        runTest(dispatcher) {
            val holding = Holding("h1", "HDFC Home Loan", HoldingKind.LIABILITY, Sector.PROPERTY, null, null)
            val vm = viewModel(Result.success(holding), Result.success(loanMeta()), Result.success(outstandingHistory(47_50_000_00L)))
            vm.load("h1")
            advanceUntilIdle()
            vm.onExtraPaymentChange("not a number")

            vm.computePrepay()

            assertNotNull(vm.uiState.value.prepayError)
        }

    @Test
    fun `computePrepay produces a projection for a valid extra payment`() =
        runTest(dispatcher) {
            val holding = Holding("h1", "HDFC Home Loan", HoldingKind.LIABILITY, Sector.PROPERTY, null, null)
            val vm = viewModel(Result.success(holding), Result.success(loanMeta()), Result.success(outstandingHistory(47_50_000_00L)))
            vm.load("h1")
            advanceUntilIdle()
            vm.onExtraPaymentChange("5,00,000")

            vm.computePrepay()

            val projection = vm.uiState.value.prepayProjection
            assertNotNull(projection)
            assertNull(vm.uiState.value.prepayError)
            assertEquals(true, projection!!.newPayoffMonths < projection.currentPayoffMonths)
        }
}
