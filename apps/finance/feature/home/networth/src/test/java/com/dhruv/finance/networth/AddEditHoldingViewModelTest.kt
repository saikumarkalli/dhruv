package com.dhruv.finance.networth

import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.core.observability.NoOpPerformanceTracer
import com.dhruv.finance.data.tracker.model.CreateHoldingRequest
import com.dhruv.finance.data.tracker.model.Holding
import com.dhruv.finance.data.tracker.model.HoldingKind
import com.dhruv.finance.data.tracker.model.HoldingWithValue
import com.dhruv.finance.data.tracker.repo.HoldingRepository
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

private class FakeAddEditHoldingRepository(
    private val onCreate: (CreateHoldingRequest) -> Result<String> = { Result.success("new-id") },
) : HoldingRepository {
    var createCallCount = 0
        private set

    override suspend fun createWithFirstValuation(request: CreateHoldingRequest): Result<String> {
        createCallCount++
        return onCreate(request)
    }

    override suspend fun list(kind: HoldingKind): Result<List<HoldingWithValue>> =
        throw UnsupportedOperationException("not exercised by this test")

    override suspend fun get(holdingId: String): Result<Holding> =
        throw UnsupportedOperationException("not exercised by this test")
}

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditHoldingViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repo: FakeAddEditHoldingRepository) =
        AddEditHoldingViewModel(repo, NoOpCrashReporter, NoOpPerformanceTracer)

    @Test
    fun `save rejects an empty name without calling the repository`() =
        runTest {
            val repo = FakeAddEditHoldingRepository()
            val vm = viewModel(repo)
            vm.onSectorChange("BANK")
            vm.onAmountChange("100")

            vm.save()

            assertEquals(0, repo.createCallCount)
            assertNotNull(vm.uiState.value.nameError)
        }

    @Test
    fun `save rejects a missing sector without calling the repository`() =
        runTest {
            val repo = FakeAddEditHoldingRepository()
            val vm = viewModel(repo)
            vm.onNameChange("HDFC Savings")
            vm.onAmountChange("100")

            vm.save()

            assertEquals(0, repo.createCallCount)
            assertNotNull(vm.uiState.value.sectorError)
        }

    @Test
    fun `save rejects a non-numeric amount without calling the repository`() =
        runTest {
            val repo = FakeAddEditHoldingRepository()
            val vm = viewModel(repo)
            vm.onNameChange("HDFC Savings")
            vm.onSectorChange("BANK")
            vm.onAmountChange("not a number")

            vm.save()

            assertEquals(0, repo.createCallCount)
            assertNotNull(vm.uiState.value.amountError)
        }

    @Test
    fun `save with valid input calls the repository once and records the new holding id`() =
        runTest(dispatcher) {
            val repo = FakeAddEditHoldingRepository(onCreate = { Result.success("new-holding-id") })
            val vm = viewModel(repo)
            vm.onNameChange("HDFC Savings")
            vm.onSectorChange("BANK")
            vm.onAmountChange("50,000")

            vm.save()
            advanceUntilIdle()

            assertEquals(1, repo.createCallCount)
            assertEquals("new-holding-id", vm.uiState.value.savedHoldingId)
            assertNull(vm.uiState.value.amountError)
        }

    @Test
    fun `parseRupeesToPaise converts a comma-formatted rupee string to paise`() {
        assertEquals(5_000_000L, parseRupeesToPaise("50,000"))
        assertEquals(50L, parseRupeesToPaise("0.50"))
        assertNull(parseRupeesToPaise("not a number"))
        assertNull(parseRupeesToPaise("-5"))
    }
}
