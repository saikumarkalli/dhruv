package com.dhruv.finance.networth

import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.core.observability.NoOpPerformanceTracer
import com.dhruv.finance.data.tracker.model.CreateHoldingRequest
import com.dhruv.finance.data.tracker.model.CreateLiabilityMetaRequest
import com.dhruv.finance.data.tracker.model.Holding
import com.dhruv.finance.data.tracker.model.HoldingKind
import com.dhruv.finance.data.tracker.model.HoldingWithValue
import com.dhruv.finance.data.tracker.model.LiabilityMeta
import com.dhruv.finance.data.tracker.model.Sector
import com.dhruv.finance.data.tracker.model.UpdateHoldingRequest
import com.dhruv.finance.data.tracker.model.UpdateLiabilityMetaRequest
import com.dhruv.finance.data.tracker.repo.HoldingRepository
import com.dhruv.finance.data.tracker.repo.LiabilityRepository
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
    private val onGet: (String) -> Result<Holding> = { throw UnsupportedOperationException("not exercised by this test") },
    private val onUpdate: (String, UpdateHoldingRequest) -> Result<Unit> = { _, _ -> Result.success(Unit) },
) : HoldingRepository {
    var createCallCount = 0
        private set
    var updateCallCount = 0
        private set
    var lastUpdateRequest: UpdateHoldingRequest? = null
        private set

    override suspend fun createWithFirstValuation(request: CreateHoldingRequest): Result<String> {
        createCallCount++
        return onCreate(request)
    }

    override suspend fun list(kind: HoldingKind): Result<List<HoldingWithValue>> =
        throw UnsupportedOperationException("not exercised by this test")

    override suspend fun get(holdingId: String): Result<Holding> = onGet(holdingId)

    override suspend fun update(
        holdingId: String,
        request: UpdateHoldingRequest,
    ): Result<Unit> {
        updateCallCount++
        lastUpdateRequest = request
        return onUpdate(holdingId, request)
    }

    override suspend fun softDelete(holdingId: String): Result<Unit> = throw UnsupportedOperationException("not exercised by this test")

    override suspend fun restore(holdingId: String): Result<Unit> = throw UnsupportedOperationException("not exercised by this test")
}

private class FakeAddEditLiabilityRepository(
    private val onCreateMeta: (CreateLiabilityMetaRequest) -> Result<Unit> = { Result.success(Unit) },
) : LiabilityRepository {
    var createMetaCallCount = 0
        private set
    var lastRequest: CreateLiabilityMetaRequest? = null
        private set

    override suspend fun createMeta(request: CreateLiabilityMetaRequest): Result<Unit> {
        createMetaCallCount++
        lastRequest = request
        return onCreateMeta(request)
    }

    override suspend fun listAll(): Result<List<LiabilityMeta>> = throw UnsupportedOperationException("not exercised by this test")

    override suspend fun get(holdingId: String): Result<LiabilityMeta> = throw UnsupportedOperationException("not exercised by this test")

    override suspend fun updateMeta(
        holdingId: String,
        request: UpdateLiabilityMetaRequest,
    ): Result<Unit> = throw UnsupportedOperationException("not exercised by this test")
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

    private fun viewModel(
        repo: FakeAddEditHoldingRepository,
        liabilityRepo: FakeAddEditLiabilityRepository = FakeAddEditLiabilityRepository(),
    ) = AddEditHoldingViewModel(repo, liabilityRepo, NoOpCrashReporter, NoOpPerformanceTracer)

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

    @Test
    fun `parseRatePercentToBps converts a percent string to basis points`() {
        assertEquals(850, parseRatePercentToBps("8.5"))
        assertEquals(0, parseRatePercentToBps("0"))
        assertNull(parseRatePercentToBps("101"))
        assertNull(parseRatePercentToBps("not a number"))
    }

    // Phase 6 scope addition: a LIABILITY-kind save rejects a missing type/rate before calling
    // either repository — mirrors the existing name/sector/amount validation gate above.
    @Test
    fun `save rejects a liability with no type or rate without calling either repository`() =
        runTest {
            val repo = FakeAddEditHoldingRepository()
            val liabilityRepo = FakeAddEditLiabilityRepository()
            val vm = viewModel(repo, liabilityRepo)
            vm.onKindChange(HoldingKind.LIABILITY)
            vm.onNameChange("HDFC Home Loan")
            vm.onSectorChange("PROPERTY")
            vm.onAmountChange("50,00,000")

            vm.save()

            assertEquals(0, repo.createCallCount)
            assertEquals(0, liabilityRepo.createMetaCallCount)
            assertNotNull(vm.uiState.value.liabilityTypeError)
            assertNotNull(vm.uiState.value.rateError)
        }

    @Test
    fun `save with a valid liability calls both repositories and records the new holding id`() =
        runTest(dispatcher) {
            val repo = FakeAddEditHoldingRepository(onCreate = { Result.success("loan-holding-id") })
            val liabilityRepo = FakeAddEditLiabilityRepository()
            val vm = viewModel(repo, liabilityRepo)
            vm.onKindChange(HoldingKind.LIABILITY)
            vm.onNameChange("HDFC Home Loan")
            vm.onSectorChange("PROPERTY")
            vm.onAmountChange("50,00,000")
            vm.onLiabilityTypeChange("HOME_LOAN")
            vm.onRateChange("8.5")
            vm.onEmiChange("45,000")
            vm.onTenureMonthsChange("240")

            vm.save()
            advanceUntilIdle()

            assertEquals(1, repo.createCallCount)
            assertEquals(1, liabilityRepo.createMetaCallCount)
            assertEquals("loan-holding-id", vm.uiState.value.savedHoldingId)
            val sentRequest = liabilityRepo.lastRequest
            assertEquals("HOME_LOAN", sentRequest?.liabilityTypeCode)
            assertEquals(850, sentRequest?.rateBps)
            assertEquals(45_000_00L, sentRequest?.emiPaise)
            assertEquals(240, sentRequest?.tenureMonths)
            assertEquals(500_000_000L, sentRequest?.originalPrincipalPaise)
            assertNull(vm.uiState.value.liabilityMetaError)
        }

    @Test
    fun `a liability-meta failure still records the saved holding id, surfaced separately`() =
        runTest(dispatcher) {
            val repo = FakeAddEditHoldingRepository(onCreate = { Result.success("loan-holding-id") })
            val liabilityRepo = FakeAddEditLiabilityRepository(onCreateMeta = { Result.failure(java.io.IOException("down")) })
            val vm = viewModel(repo, liabilityRepo)
            vm.onKindChange(HoldingKind.LIABILITY)
            vm.onNameChange("HDFC Home Loan")
            vm.onSectorChange("PROPERTY")
            vm.onAmountChange("50,00,000")
            vm.onLiabilityTypeChange("HOME_LOAN")
            vm.onRateChange("8.5")

            vm.save()
            advanceUntilIdle()

            assertEquals("loan-holding-id", vm.uiState.value.savedHoldingId)
            assertNotNull(vm.uiState.value.liabilityMetaError)
        }

    // Phase 9, T051/T052: C4's edit path — a mistakenly-added holding was previously only
    // correctable via full-account erasure; startEditing prefills, save() calls update() not
    // createWithFirstValuation().
    @Test
    fun `startEditing prefills state from the existing holding`() =
        runTest(dispatcher) {
            val holding = Holding("h1", "HDFC Savings", HoldingKind.ASSET, Sector.BANK, 10_000_00L, "Joint account")
            val repo = FakeAddEditHoldingRepository(onGet = { Result.success(holding) })
            val vm = viewModel(repo)

            vm.startEditing("h1")
            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals(true, state.isEditing)
            assertEquals("HDFC Savings", state.name)
            assertEquals("BANK", state.sectorCode)
            assertEquals("10000", state.investedAmountText)
            assertEquals("Joint account", state.notesText)
        }

    @Test
    fun `save in edit mode calls update, never createWithFirstValuation`() =
        runTest(dispatcher) {
            val holding = Holding("h1", "HDFC Savings", HoldingKind.ASSET, Sector.BANK, null, null)
            val repo = FakeAddEditHoldingRepository(onGet = { Result.success(holding) })
            val vm = viewModel(repo)
            vm.startEditing("h1")
            advanceUntilIdle()
            vm.onNameChange("HDFC Savings (renamed)")

            vm.save()
            advanceUntilIdle()

            assertEquals(0, repo.createCallCount)
            assertEquals(1, repo.updateCallCount)
            assertEquals("HDFC Savings (renamed)", repo.lastUpdateRequest?.name)
            assertEquals("BANK", repo.lastUpdateRequest?.sectorCode)
            assertEquals("h1", vm.uiState.value.savedHoldingId)
        }

    @Test
    fun `save in edit mode rejects an empty name without calling update`() =
        runTest(dispatcher) {
            val holding = Holding("h1", "HDFC Savings", HoldingKind.ASSET, Sector.BANK, null, null)
            val repo = FakeAddEditHoldingRepository(onGet = { Result.success(holding) })
            val vm = viewModel(repo)
            vm.startEditing("h1")
            advanceUntilIdle()
            vm.onNameChange("")

            vm.save()

            assertEquals(0, repo.updateCallCount)
            assertNotNull(vm.uiState.value.nameError)
        }
}
