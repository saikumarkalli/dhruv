package com.dhruv.finance.money

import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.core.observability.NoOpPerformanceTracer
import com.dhruv.finance.data.tracker.model.Account
import com.dhruv.finance.data.tracker.model.AccountType
import com.dhruv.finance.data.tracker.model.Category
import com.dhruv.finance.data.tracker.model.CategoryKind
import com.dhruv.finance.data.tracker.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** MNY-FLOW-001 (D2 -> D3 carry-over) and N4 (confirm-on-discard, backed by [isDirty] —
 * `rememberDiscardGuard` reads this flag; the Compose-level confirm dialog itself is exercised by
 * `DiscardGuard`'s own logic, not re-tested here). */
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionFormViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val bankAccount = Account("acc-1", "Checking", AccountType.BANK, null, true, null, null, 0, null)
    private val walletAccount = bankAccount.copy(id = "acc-2", name = "Wallet")
    private val groceries = Category("cat-1", "Groceries", CategoryKind.EXPENSE, null, null, false)

    private fun viewModel(
        transactionRepository: FakeTransactionRepository = FakeTransactionRepository(),
        accountRepository: FakeAccountRepository = FakeAccountRepository(listOf(bankAccount, walletAccount)),
        categoryRepository: FakeCategoryRepository = FakeCategoryRepository(listOf(groceries)),
        recurringRepository: FakeRecurringRepository = FakeRecurringRepository(),
    ) = TransactionFormViewModel(
        transactionRepository,
        accountRepository,
        categoryRepository,
        recurringRepository,
        NoOpCrashReporter,
        NoOpPerformanceTracer,
    )

    // D2 "more options" hands off everything already entered (spec.md Story 1, Acceptance
    // Scenario 4) — carried-over values arrive already-filled and NOT dirty (nothing new typed yet).
    @Test
    fun `opening with a prefill carries D2's values over and starts clean, not dirty`() =
        runTest {
            val vm = viewModel()
            val prefill =
                TransactionFormUiState(
                    type = TransactionType.EXPENSE,
                    amountPaise = 12_00,
                    accountId = "acc-1",
                    categoryId = "cat-1",
                    isDirty = true,
                )

            vm.open(prefill)
            advanceUntilIdle()

            assertEquals(12_00L, vm.uiState.value.amountPaise)
            assertEquals("acc-1", vm.uiState.value.accountId)
            assertEquals("cat-1", vm.uiState.value.categoryId)
            assertFalse("a freshly opened form must not already read as dirty", vm.uiState.value.isDirty)
        }

    @Test
    fun `every field setter marks the form dirty`() =
        runTest {
            val vm = viewModel()
            assertFalse(vm.uiState.value.isDirty)

            vm.setAmount(500)

            assertTrue(vm.uiState.value.isDirty)
        }

    @Test
    fun `a successful save clears the dirty flag`() =
        runTest {
            val vm = viewModel()
            vm.setAmount(5_00)
            vm.setAccount("acc-1")
            vm.setCategory("cat-1")
            assertTrue(vm.uiState.value.isDirty)

            vm.save()
            advanceUntilIdle()

            assertFalse(vm.uiState.value.isDirty)
            assertNotNull(vm.uiState.value.savedTransactionId)
        }

    @Test
    fun `save without an account sets a validation error and does not save`() =
        runTest {
            val transactionRepository = FakeTransactionRepository()
            val vm = viewModel(transactionRepository = transactionRepository)
            vm.setAmount(5_00)
            vm.setCategory("cat-1")

            vm.save()
            advanceUntilIdle()

            assertEquals(0, transactionRepository.created.size)
            assertEquals("Choose an account", vm.uiState.value.validationError)
        }

    @Test
    fun `a TRANSFER without a destination account is rejected with a validation error`() =
        runTest {
            val transactionRepository = FakeTransactionRepository()
            val vm = viewModel(transactionRepository = transactionRepository)
            vm.setType(TransactionType.TRANSFER)
            vm.setAmount(10_00)
            vm.setAccount("acc-1")

            vm.save()
            advanceUntilIdle()

            assertEquals(0, transactionRepository.created.size)
            assertEquals("Choose a destination account", vm.uiState.value.validationError)
        }

    @Test
    fun `a TRANSFER whose destination equals its source is rejected`() =
        runTest {
            val transactionRepository = FakeTransactionRepository()
            val vm = viewModel(transactionRepository = transactionRepository)
            vm.setType(TransactionType.TRANSFER)
            vm.setAmount(10_00)
            vm.setAccount("acc-1")
            vm.setToAccount("acc-1")

            vm.save()
            advanceUntilIdle()

            assertEquals(0, transactionRepository.created.size)
            assertNotNull(vm.uiState.value.validationError)
        }

    @Test
    fun `a well-formed TRANSFER saves successfully`() =
        runTest {
            val transactionRepository = FakeTransactionRepository()
            val vm = viewModel(transactionRepository = transactionRepository)
            vm.setType(TransactionType.TRANSFER)
            vm.setAmount(10_00)
            vm.setAccount("acc-1")
            vm.setToAccount("acc-2")

            vm.save()
            advanceUntilIdle()

            assertEquals(1, transactionRepository.created.size)
            assertEquals(TransactionType.TRANSFER, transactionRepository.created.single().type)
        }

    @Test
    fun `open with no prefill loads account and category options`() =
        runTest {
            val vm = viewModel()

            vm.open()
            advanceUntilIdle()

            assertEquals(listOf("acc-1", "acc-2"), vm.uiState.value.accountOptions.map { it.id })
            assertEquals(listOf("cat-1"), vm.uiState.value.categoryOptions.map { it.id })
        }

    // MNY-FLOW-002 (T065/T068): saving with "make it recurring" writes a recurring_templates row
    // and no duplicate immediate transaction (FR-027).
    @Test
    fun `saving with make-it-recurring on writes only a recurring template, never a transaction`() =
        runTest {
            val transactionRepository = FakeTransactionRepository()
            val recurringRepository = FakeRecurringRepository()
            val vm = viewModel(transactionRepository = transactionRepository, recurringRepository = recurringRepository)
            vm.setAmount(1_500_00)
            vm.setAccount("acc-1")
            vm.setCategory("cat-1")
            vm.setMakeRecurring(true)
            vm.setRrule("FREQ=MONTHLY")

            vm.save()
            advanceUntilIdle()

            assertEquals(0, transactionRepository.created.size)
            assertEquals(1, recurringRepository.createdFromTransaction.size)
            assertEquals(1_500_00L, recurringRepository.createdFromTransaction.single().amountPaise)
            assertTrue(vm.uiState.value.savedRecurringTemplateId != null)
        }

    @Test
    fun `saving with make-it-recurring off writes only a transaction, no recurring template`() =
        runTest {
            val transactionRepository = FakeTransactionRepository()
            val recurringRepository = FakeRecurringRepository()
            val vm = viewModel(transactionRepository = transactionRepository, recurringRepository = recurringRepository)
            vm.setAmount(200_00)
            vm.setAccount("acc-1")
            vm.setCategory("cat-1")

            vm.save()
            advanceUntilIdle()

            assertEquals(1, transactionRepository.created.size)
            assertEquals(0, recurringRepository.createdFromTransaction.size)
        }

    // T097: a retry after a failed save reuses the same request_id.
    @Test
    fun `retrying a failed save reuses the same request id`() =
        runTest {
            var shouldFail = true
            val transactionRepository =
                FakeTransactionRepository(
                    createResult = { txn ->
                        if (shouldFail) {
                            Result.failure(java.io.IOException("timeout"))
                        } else {
                            Result.success(txn.copy(id = "txn-1"))
                        }
                    },
                )
            val vm = viewModel(transactionRepository = transactionRepository)
            vm.setAmount(200_00)
            vm.setAccount("acc-1")
            vm.setCategory("cat-1")

            vm.save()
            advanceUntilIdle()
            shouldFail = false
            vm.save()
            advanceUntilIdle()

            assertEquals(2, transactionRepository.createRequestIds.size)
            assertEquals(transactionRepository.createRequestIds[0], transactionRepository.createRequestIds[1])
            assertEquals("txn-1", vm.uiState.value.savedTransactionId)
        }

    // Same fix as QuickAddViewModelTest's -- see its doc comment (found 2026-09-05, live-device
    // audit). D3 is also reachable before a user has ever visited D8.
    @Test
    fun `open ensures the reserved categories exist`() =
        runTest {
            val categoryRepository = FakeCategoryRepository(listOf(groceries))
            val vm = viewModel(categoryRepository = categoryRepository)

            vm.open()
            advanceUntilIdle()

            assertEquals(1, categoryRepository.ensureReservedCalls.size)
        }
}
