package com.dhruv.finance.networth

import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.core.observability.NoOpPerformanceTracer
import com.dhruv.finance.data.tracker.model.ValuationHistoryEntry
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

private class FakeAddValuationRepository(
    private val onRecord: (holdingId: String, valuePaise: Long, asOf: String, sourceCode: String, requestId: String?) -> Result<String> =
        { _, _, _, _, _ -> Result.success("recorded-id") },
    private val onCorrect: (valuationId: String, valuePaise: Long, asOf: String, note: String?) -> Result<String> =
        { _, _, _, _ -> Result.success("corrected-id") },
) : ValuationRepository {
    var recordCallCount = 0
        private set
    var correctCallCount = 0
        private set

    override suspend fun listHistory(holdingId: String): Result<List<ValuationHistoryEntry>> =
        throw UnsupportedOperationException("not exercised by this test")

    override suspend fun recordValue(
        holdingId: String,
        valuePaise: Long,
        asOf: String,
        sourceCode: String,
        requestId: String?,
    ): Result<String> {
        recordCallCount++
        return onRecord(holdingId, valuePaise, asOf, sourceCode, requestId)
    }

    override suspend fun correctValue(
        valuationId: String,
        valuePaise: Long,
        asOf: String,
        note: String?,
    ): Result<String> {
        correctCallCount++
        return onCorrect(valuationId, valuePaise, asOf, note)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AddValuationViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repo: ValuationRepository) = AddValuationViewModel(repo, NoOpCrashReporter, NoOpPerformanceTracer)

    @Test
    fun `save with no correcting id calls recordValue, never correctValue`() =
        runTest(dispatcher) {
            val repo = FakeAddValuationRepository()
            val vm = viewModel(repo)
            vm.start(holdingId = "h1", lastValuePaise = 4_00_000_00L)
            vm.onAmountChange("500000")

            vm.save()
            advanceUntilIdle()

            assertEquals(1, repo.recordCallCount)
            assertEquals(0, repo.correctCallCount)
            assertEquals("recorded-id", vm.uiState.value.savedValuationId)
        }

    @Test
    fun `save with a correcting id calls correctValue, never recordValue`() =
        runTest(dispatcher) {
            val repo = FakeAddValuationRepository()
            val vm = viewModel(repo)
            vm.start(holdingId = "h1", lastValuePaise = 4_00_000_00L, correctingValuationId = "v-wrong")
            vm.onAmountChange("475000")

            vm.save()
            advanceUntilIdle()

            assertEquals(0, repo.recordCallCount)
            assertEquals(1, repo.correctCallCount)
            assertEquals("corrected-id", vm.uiState.value.savedValuationId)
        }

    @Test
    fun `save rejects an invalid amount without calling the repository`() =
        runTest(dispatcher) {
            val repo = FakeAddValuationRepository()
            val vm = viewModel(repo)
            vm.start(holdingId = "h1", lastValuePaise = 100L)
            vm.onAmountChange("not a number")

            vm.save()
            advanceUntilIdle()

            assertEquals(0, repo.recordCallCount)
            assertEquals(0, repo.correctCallCount)
            assertEquals("Enter a valid amount", vm.uiState.value.amountError)
        }

    @Test
    fun `previewDelta is null until the amount parses`() {
        val vm = viewModel(FakeAddValuationRepository())
        vm.start(holdingId = "h1", lastValuePaise = 4_00_000_00L)
        vm.onAmountChange("not a number")

        assertNull(vm.previewDelta(vm.uiState.value))
    }

    @Test
    fun `previewDelta computes delta paise and basis points against lastValuePaise`() {
        val vm = viewModel(FakeAddValuationRepository())
        vm.start(holdingId = "h1", lastValuePaise = 4_00_000_00L)
        vm.onAmountChange("500000") // 5,00,000.00 rupees = 5_00_000_00 paise

        val (deltaPaise, deltaBps) = vm.previewDelta(vm.uiState.value)!!

        assertEquals(1_00_000_00L, deltaPaise)
        assertEquals(2500, deltaBps) // +1,00,000 / 4,00,000 = 25% = 2500 bps
    }
}
