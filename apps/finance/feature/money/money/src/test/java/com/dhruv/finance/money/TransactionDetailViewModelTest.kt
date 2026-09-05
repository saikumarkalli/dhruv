package com.dhruv.finance.money

import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.core.observability.NoOpPerformanceTracer
import com.dhruv.finance.data.tracker.model.Account
import com.dhruv.finance.data.tracker.model.AccountType
import com.dhruv.finance.data.tracker.model.Category
import com.dhruv.finance.data.tracker.model.CategoryKind
import com.dhruv.finance.data.tracker.model.Transaction
import com.dhruv.finance.data.tracker.model.TransactionEvent
import com.dhruv.finance.data.tracker.model.TransactionEventKind
import com.dhruv.finance.data.tracker.model.TransactionSource
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * T049/T050 — `MNY-UI-006` (read-first detail + ordered history) and `MNY-FLOW-004`/`MNY-FLOW-005`
 * (Duplicate/Make-recurring). Make-recurring itself is a plain callback on the Compose screen
 * (`onMakeRecurring: (Transaction) -> Unit`, T053's boundary note) — this module has no
 * Robolectric/compose-ui-test dependency (see `build.gradle.kts`), so "the right transaction" is
 * verified here at the ViewModel/state level: the [TransactionDetailUiState.Loaded.transaction]
 * the screen would hand to that callback is asserted to be exactly the loaded transaction, unchanged.
 * Firing the actual Compose callback is out of this file's reach, same honesty note as
 * `TransactionAuditTest`'s top comment.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val groceries = Category("cat-1", "Groceries", CategoryKind.EXPENSE, null, null, false)
    private val shopping = Category("cat-2", "Shopping", CategoryKind.EXPENSE, null, null, false)
    private val checking = Account("acc-1", "Checking", AccountType.BANK, null, true, null, null, 0, null)
    private val wallet = Account("acc-2", "Wallet", AccountType.CASH, null, false, null, null, 0, null)

    private val baseTransaction =
        Transaction(
            id = "txn-1",
            type = TransactionType.EXPENSE,
            amountPaise = 50_00,
            accountId = "acc-1",
            toAccountId = null,
            categoryId = "cat-1",
            payee = "Coffee shop",
            note = "Team catch-up",
            occurredAt = Instant.parse("2026-09-01T10:00:00Z"),
            cleared = true,
            receiptPath = null,
            goalId = null,
            recurringId = null,
            splitGroupId = null,
            source = TransactionSource.MANUAL,
        )

    private fun viewModel(
        transactions: List<Transaction> = listOf(baseTransaction),
        events: Map<String, List<TransactionEvent>> = emptyMap(),
        transactionRepository: FakeTransactionRepository =
            FakeTransactionRepository(transactions = transactions, events = events),
        accountRepository: FakeAccountRepository = FakeAccountRepository(listOf(checking, wallet)),
        categoryRepository: FakeCategoryRepository = FakeCategoryRepository(listOf(groceries, shopping)),
    ) = TransactionDetailViewModel(
        transactionRepository,
        accountRepository,
        categoryRepository,
        NoOpCrashReporter,
        NoOpPerformanceTracer,
    ) to transactionRepository

    // FR-009: amount/payee/date-time/cleared/category/account render from the loaded transaction,
    // without any edit-mode entry point.
    @Test
    fun `load renders amount, payee, cleared state, category and account without an edit mode`() =
        runTest {
            val (vm, _) = viewModel()

            vm.load("txn-1")
            advanceUntilIdle()

            val state = vm.uiState.value as TransactionDetailUiState.Loaded
            assertEquals(50_00L, state.transaction.amountPaise)
            assertEquals("Coffee shop", state.transaction.payee)
            assertEquals(Instant.parse("2026-09-01T10:00:00Z"), state.transaction.occurredAt)
            assertTrue(state.transaction.cleared)
            assertEquals("Groceries", state.categoryName)
            assertEquals("Checking", state.accountName)
        }

    // FR-007/FR-009: HISTORY lists every event in order, in plain language, naming what changed.
    @Test
    fun `history lists every event in order with plain-language lines`() =
        runTest {
            val history =
                listOf(
                    TransactionEvent(
                        id = "evt-1",
                        transactionId = "txn-1",
                        at = Instant.parse("2026-09-01T10:00:00Z"),
                        kind = TransactionEventKind.CREATED,
                        detail = null,
                    ),
                    TransactionEvent(
                        id = "evt-2",
                        transactionId = "txn-1",
                        at = Instant.parse("2026-09-02T10:00:00Z"),
                        kind = TransactionEventKind.CATEGORY_CHANGED,
                        detail = mapOf("old_category_id" to "cat-1", "new_category_id" to "cat-2"),
                    ),
                    TransactionEvent(
                        id = "evt-3",
                        transactionId = "txn-1",
                        at = Instant.parse("2026-09-03T10:00:00Z"),
                        kind = TransactionEventKind.EDITED,
                        detail =
                            mapOf(
                                "amount_paise" to mapOf("old" to 50_00, "new" to 75_00),
                                "payee" to mapOf("old" to "Coffee shop", "new" to "Cafe"),
                            ),
                    ),
                )
            val (vm, _) = viewModel(events = mapOf("txn-1" to history))

            vm.load("txn-1")
            advanceUntilIdle()

            val state = vm.uiState.value as TransactionDetailUiState.Loaded
            assertEquals(3, state.history.size)
            assertEquals(listOf("Created"), state.history[0].lines)
            assertEquals(listOf("Category changed from Groceries to Shopping"), state.history[1].lines)
            assertEquals(2, state.history[2].lines.size)
            assertTrue(state.history[2].lines.any { it.contains("Amount changed from") && it.contains("75.00") })
            assertTrue(state.history[2].lines.any { it.contains("Payee changed from Coffee shop to Cafe") })
        }

    // spec Edge Cases: Duplicate must not chain the original's history — it hands back a brand-new,
    // unsaved TransactionFormUiState and writes nothing until the caller explicitly saves it.
    @Test
    fun `duplicateDraft returns an unsaved prefilled draft and writes nothing`() =
        runTest {
            val (vm, repo) = viewModel()
            vm.load("txn-1")
            advanceUntilIdle()
            val transaction = (vm.uiState.value as TransactionDetailUiState.Loaded).transaction

            val draft = vm.duplicateDraft(transaction)

            assertEquals(transaction.type, draft.type)
            assertEquals(transaction.amountPaise, draft.amountPaise)
            assertEquals(transaction.accountId, draft.accountId)
            assertEquals(transaction.categoryId, draft.categoryId)
            assertEquals(transaction.payee, draft.payee)
            assertEquals(transaction.note, draft.note)
            assertEquals(transaction.cleared, draft.cleared)
            assertEquals("a fresh draft must not read as already saved", null, draft.savedTransactionId)
            assertEquals("a fresh draft must not carry the original's dirty state", false, draft.isDirty)
            assertTrue("Duplicate must write nothing until the caller saves it", repo.created.isEmpty())
        }

    // FR-010: "make it recurring" opens pre-filled from the original transaction — the screen wires
    // this as `onMakeRecurring(state.transaction)`, so proving the loaded state's transaction is
    // exactly the one this screen holds is what a ViewModel-level test can honestly assert (see the
    // class doc comment).
    @Test
    fun `the loaded transaction is exactly what Make-recurring would be pre-filled from`() =
        runTest {
            val (vm, _) = viewModel()

            vm.load("txn-1")
            advanceUntilIdle()

            val state = vm.uiState.value as TransactionDetailUiState.Loaded
            assertEquals(baseTransaction, state.transaction)
        }

    @Test
    fun `loading a transaction id that does not exist renders an error state, not a blank screen`() =
        runTest {
            val (vm, _) = viewModel(transactions = emptyList())

            vm.load("missing-txn")
            advanceUntilIdle()

            assertTrue(vm.uiState.value is TransactionDetailUiState.Error)
        }
}
