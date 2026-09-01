package com.dhruv.finance.networth

import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.core.observability.NoOpPerformanceTracer
import com.dhruv.finance.data.tracker.model.CreateHoldingRequest
import com.dhruv.finance.data.tracker.model.Holding
import com.dhruv.finance.data.tracker.model.HoldingKind
import com.dhruv.finance.data.tracker.model.HoldingWithValue
import com.dhruv.finance.data.tracker.model.Sector
import com.dhruv.finance.data.tracker.model.Valuation
import com.dhruv.finance.data.tracker.model.ValuationHistoryEntry
import com.dhruv.finance.data.tracker.model.ValuationSource
import com.dhruv.finance.data.tracker.repo.HoldingRepository
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

private class FakeHoldingDetailHoldingRepository(
    private val result: Result<Holding>,
) : HoldingRepository {
    override suspend fun createWithFirstValuation(request: CreateHoldingRequest): Result<String> =
        throw UnsupportedOperationException("not exercised by this test")

    override suspend fun list(kind: HoldingKind): Result<List<HoldingWithValue>> =
        throw UnsupportedOperationException("not exercised by this test")

    override suspend fun get(holdingId: String): Result<Holding> = result
}

private class FakeHoldingDetailValuationRepository(
    private val result: Result<List<ValuationHistoryEntry>>,
) : ValuationRepository {
    override suspend fun listHistory(holdingId: String): Result<List<ValuationHistoryEntry>> = result

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

@OptIn(ExperimentalCoroutinesApi::class)
class HoldingDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun entry(
        valuePaise: Long,
        asOf: String,
    ) = ValuationHistoryEntry(
        valuation = Valuation("v-$asOf", "h1", valuePaise, asOf, ValuationSource.MANUAL),
        deltaPaise = null,
        deltaPercentBps = null,
    )

    @Test
    fun `load populates the holding and history`() =
        runTest(dispatcher) {
            val holding = Holding("h1", "HDFC Savings", HoldingKind.ASSET, Sector.BANK, null, null)
            val history = listOf(entry(5_00_000_00L, "2026-08-01"), entry(4_50_000_00L, "2026-07-01"))
            val vm =
                HoldingDetailViewModel(
                    holdingRepository = FakeHoldingDetailHoldingRepository(Result.success(holding)),
                    valuationRepository = FakeHoldingDetailValuationRepository(Result.success(history)),
                    crashReporter = NoOpCrashReporter,
                    performanceTracer = NoOpPerformanceTracer,
                )

            vm.load("h1")
            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals(false, state.isLoading)
            assertEquals("HDFC Savings", state.holding?.name)
            assertEquals(2, state.history.size)
            assertNull(state.errorMessage)
        }

    @Test
    fun `load failure surfaces an error message`() =
        runTest(dispatcher) {
            val vm =
                HoldingDetailViewModel(
                    holdingRepository = FakeHoldingDetailHoldingRepository(Result.failure(NoSuchElementException("gone"))),
                    valuationRepository = FakeHoldingDetailValuationRepository(Result.success(emptyList())),
                    crashReporter = NoOpCrashReporter,
                    performanceTracer = NoOpPerformanceTracer,
                )

            vm.load("missing")
            advanceUntilIdle()

            assertNotNull(vm.uiState.value.errorMessage)
            assertEquals(false, vm.uiState.value.isLoading)
        }

    @Test
    fun `trendValuesPaise returns oldest-first for chart order`() =
        runTest(dispatcher) {
            val holding = Holding("h1", "HDFC Savings", HoldingKind.ASSET, Sector.BANK, null, null)
            // Newest-first, as the repository returns it.
            val history = listOf(entry(300L, "2026-08-01"), entry(200L, "2026-07-01"), entry(100L, "2026-06-01"))
            val vm =
                HoldingDetailViewModel(
                    holdingRepository = FakeHoldingDetailHoldingRepository(Result.success(holding)),
                    valuationRepository = FakeHoldingDetailValuationRepository(Result.success(history)),
                    crashReporter = NoOpCrashReporter,
                    performanceTracer = NoOpPerformanceTracer,
                )

            vm.load("h1")
            advanceUntilIdle()
            vm.setTrendRange(HoldingDetailViewModel.TrendRange.ALL)

            val points = vm.trendValuesPaise(vm.uiState.value)

            assertEquals(listOf(100L, 200L, 300L), points)
        }
}
