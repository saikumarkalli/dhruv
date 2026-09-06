package com.dhruv.finance.money

import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.core.observability.NoOpPerformanceTracer
import com.dhruv.finance.data.tracker.model.Account
import com.dhruv.finance.data.tracker.model.AccountType
import com.dhruv.finance.data.tracker.model.Category
import com.dhruv.finance.data.tracker.model.CategoryKind
import com.dhruv.finance.data.tracker.model.TransactionType
import com.dhruv.finance.data.tracker.repo.TransactionGuess
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

/** MNY-FLOW-001 (spec.md Story 1, Acceptance Scenario 1): category/account are pre-guessed on
 * open, both remain editable before save, and Save reaches a saved transaction. */
@OptIn(ExperimentalCoroutinesApi::class)
class QuickAddViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val bankAccount = Account("acc-1", "Checking", AccountType.BANK, null, true, null, null, 0, null)
    private val groceries = Category("cat-1", "Groceries", CategoryKind.EXPENSE, null, null, false)

    private fun viewModel(
        transactionRepository: FakeTransactionRepository = FakeTransactionRepository(),
        accountRepository: FakeAccountRepository = FakeAccountRepository(listOf(bankAccount)),
        categoryRepository: FakeCategoryRepository = FakeCategoryRepository(listOf(groceries)),
    ) = QuickAddViewModel(transactionRepository, accountRepository, categoryRepository, NoOpCrashReporter, NoOpPerformanceTracer)

    @Test
    fun `open pre-guesses category and account from the repository`() =
        runTest {
            val transactionRepository =
                FakeTransactionRepository(guess = TransactionGuess(accountId = "acc-1", categoryId = "cat-1"))
            val vm = viewModel(transactionRepository = transactionRepository)

            vm.open()
            advanceUntilIdle()

            assertEquals("acc-1", vm.uiState.value.accountId)
            assertEquals("cat-1", vm.uiState.value.categoryId)
        }

    @Test
    fun `a guessed category or account remains editable before save`() =
        runTest {
            val secondAccount = bankAccount.copy(id = "acc-2", name = "Wallet")
            val transactionRepository =
                FakeTransactionRepository(guess = TransactionGuess(accountId = "acc-1", categoryId = "cat-1"))
            val vm =
                viewModel(
                    transactionRepository = transactionRepository,
                    accountRepository = FakeAccountRepository(listOf(bankAccount, secondAccount)),
                )
            vm.open()
            advanceUntilIdle()

            vm.setAccount("acc-2")

            assertEquals("acc-2", vm.uiState.value.accountId)
        }

    @Test
    fun `open also loads account and category options for the pickers`() =
        runTest {
            val vm = viewModel()

            vm.open()
            advanceUntilIdle()

            assertEquals(
                listOf("acc-1"),
                vm.uiState.value.accountOptions
                    .map { it.id },
            )
            assertEquals(
                listOf("cat-1"),
                vm.uiState.value.categoryOptions
                    .map { it.id },
            )
        }

    @Test
    fun `save with amount, account and category reaches a saved transaction`() =
        runTest {
            val transactionRepository = FakeTransactionRepository()
            val vm = viewModel(transactionRepository = transactionRepository)
            vm.setAmount(5_000)
            vm.setAccount("acc-1")
            vm.setCategory("cat-1")

            vm.save()
            advanceUntilIdle()

            assertNotNull(vm.uiState.value.savedTransactionId)
            assertEquals(1, transactionRepository.created.size)
            assertEquals(5_000L, transactionRepository.created.single().amountPaise)
            assertEquals(TransactionType.EXPENSE, transactionRepository.created.single().type)
        }

    @Test
    fun `save is a no-op without an amount`() =
        runTest {
            val transactionRepository = FakeTransactionRepository()
            val vm = viewModel(transactionRepository = transactionRepository)
            vm.setAccount("acc-1")
            vm.setCategory("cat-1")

            vm.save()
            advanceUntilIdle()

            assertNull(vm.uiState.value.savedTransactionId)
            assertEquals(0, transactionRepository.created.size)
        }

    @Test
    fun `save is a no-op without an account`() =
        runTest {
            val transactionRepository = FakeTransactionRepository()
            val vm = viewModel(transactionRepository = transactionRepository)
            vm.setAmount(5_000)
            vm.setCategory("cat-1")

            vm.save()
            advanceUntilIdle()

            assertNull(vm.uiState.value.savedTransactionId)
            assertEquals(0, transactionRepository.created.size)
        }

    @Test
    fun `an EXPENSE save without a category is a no-op`() =
        runTest {
            val transactionRepository = FakeTransactionRepository()
            val vm = viewModel(transactionRepository = transactionRepository)
            vm.setAmount(5_000)
            vm.setAccount("acc-1")

            vm.save()
            advanceUntilIdle()

            assertNull(vm.uiState.value.savedTransactionId)
            assertEquals(0, transactionRepository.created.size)
        }

    // T097: a retry after a failed save reuses the same request_id — the whole point of
    // transactions.request_id unique is defeated if a retry mints a fresh one.
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
            vm.setAmount(5_000)
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

    // Found 2026-09-05, live-device audit: this was the only Money entry point that never seeded
    // the reserved categories -- a brand-new user's actual first action is this FAB, so every real
    // account hit an empty category picker with no way to proceed until this was fixed.
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
