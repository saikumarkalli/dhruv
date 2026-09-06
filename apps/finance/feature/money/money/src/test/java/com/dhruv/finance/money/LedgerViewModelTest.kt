package com.dhruv.finance.money

import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.core.observability.NoOpPerformanceTracer
import com.dhruv.finance.data.tracker.model.Transaction
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

/** MNY-UI-002 (day-grouping + pinned summary) and MNY-UI-003 (live filter/search result count —
 * the count-before-apply half lives in [LedgerViewModel.previewCount], tested here; the sheet UI
 * itself is Compose and not unit-tested at this layer). */
@OptIn(ExperimentalCoroutinesApi::class)
class LedgerViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun txn(
        id: String,
        occurredAt: Instant,
        amountPaise: Long,
        type: TransactionType = TransactionType.EXPENSE,
        payee: String? = null,
        categoryId: String? = "cat-1",
    ) = Transaction(
        id = id,
        type = type,
        amountPaise = amountPaise,
        accountId = "acc-1",
        toAccountId = null,
        categoryId = categoryId,
        payee = payee,
        note = null,
        occurredAt = occurredAt,
        cleared = true,
        receiptPath = null,
        goalId = null,
        recurringId = null,
        splitGroupId = null,
        source = TransactionSource.MANUAL,
    )

    private fun viewModel(transactionRepository: FakeTransactionRepository) =
        LedgerViewModel(transactionRepository, NoOpCrashReporter, NoOpPerformanceTracer)

    @Test
    fun `rows are grouped by day with a correct per-day net`() =
        runTest {
            val day1 = txn("t1", Instant.parse("2026-09-01T09:00:00Z"), 1_00, type = TransactionType.INCOME)
            val day1b = txn("t2", Instant.parse("2026-09-01T18:00:00Z"), 40, type = TransactionType.EXPENSE)
            val day2 = txn("t3", Instant.parse("2026-09-02T09:00:00Z"), 2_00, type = TransactionType.EXPENSE)
            val vm = viewModel(FakeTransactionRepository(transactions = listOf(day1, day1b, day2)))
            advanceUntilIdle()

            val loaded = vm.uiState.value as LedgerUiState.Loaded
            assertEquals(2, loaded.dayGroups.size)
            val firstDay = loaded.dayGroups.first { it.transactions.any { t -> t.id == "t1" } }
            assertEquals(60L, firstDay.netPaise) // +100 income, -40 expense
        }

    @Test
    fun `search narrows the visible list by payee`() =
        runTest {
            val coffee = txn("t1", Instant.parse("2026-09-01T09:00:00Z"), 1_00, payee = "Coffee shop")
            val rent = txn("t2", Instant.parse("2026-09-01T10:00:00Z"), 5_00, payee = "Landlord")
            val vm = viewModel(FakeTransactionRepository(transactions = listOf(coffee, rent)))
            advanceUntilIdle()

            vm.setSearchQuery("coffee")
            advanceUntilIdle()

            val loaded = vm.uiState.value as LedgerUiState.Loaded
            assertEquals(1, loaded.totalCount)
            assertTrue(
                loaded.dayGroups
                    .single()
                    .transactions
                    .single()
                    .id == "t1",
            )
        }

    @Test
    fun `applying a filter narrows the list and the shown count matches`() =
        runTest {
            val expense = txn("t1", Instant.parse("2026-09-01T09:00:00Z"), 1_00, type = TransactionType.EXPENSE)
            val income = txn("t2", Instant.parse("2026-09-01T10:00:00Z"), 5_00, type = TransactionType.INCOME, categoryId = null)
            val vm = viewModel(FakeTransactionRepository(transactions = listOf(expense, income)))
            advanceUntilIdle()

            vm.setFilter(LedgerFilter(type = TransactionType.INCOME))
            advanceUntilIdle()

            val loaded = vm.uiState.value as LedgerUiState.Loaded
            assertEquals(1, loaded.totalCount)
            assertEquals(
                "t2",
                loaded.dayGroups
                    .single()
                    .transactions
                    .single()
                    .id,
            )
        }

    // MNY-UI-003: the count previewCount() returns for a candidate filter matches what applying
    // that exact filter later produces (FR-014's "the count shown before applying matches after").
    @Test
    fun `previewCount for a candidate filter matches the count after actually applying it`() =
        runTest {
            val a = txn("t1", Instant.parse("2026-09-01T09:00:00Z"), 1_00, type = TransactionType.EXPENSE)
            val b = txn("t2", Instant.parse("2026-09-01T10:00:00Z"), 2_00, type = TransactionType.EXPENSE)
            val c = txn("t3", Instant.parse("2026-09-01T11:00:00Z"), 3_00, type = TransactionType.INCOME, categoryId = null)
            val vm = viewModel(FakeTransactionRepository(transactions = listOf(a, b, c)))
            advanceUntilIdle()

            val candidate = LedgerFilter(type = TransactionType.EXPENSE)
            val previewed = vm.previewCount(candidate)

            vm.setFilter(candidate)
            advanceUntilIdle()
            val loaded = vm.uiState.value as LedgerUiState.Loaded

            assertEquals(previewed, loaded.totalCount)
        }

    @Test
    fun `resetFilter clears back to the full unfiltered list`() =
        runTest {
            val a = txn("t1", Instant.parse("2026-09-01T09:00:00Z"), 1_00)
            val b = txn("t2", Instant.parse("2026-09-01T10:00:00Z"), 2_00, type = TransactionType.INCOME, categoryId = null)
            val vm = viewModel(FakeTransactionRepository(transactions = listOf(a, b)))
            advanceUntilIdle()
            vm.setFilter(LedgerFilter(type = TransactionType.INCOME))
            advanceUntilIdle()

            vm.resetFilter()
            advanceUntilIdle()

            val loaded = vm.uiState.value as LedgerUiState.Loaded
            assertEquals(2, loaded.totalCount)
        }

    // FR-006/DESIGN-SYSTEM §8: delete removes the row; undoDelete restores the same row.
    @Test
    fun `delete removes the transaction and undoDelete restores it`() =
        runTest {
            val a = txn("t1", Instant.parse("2026-09-01T09:00:00Z"), 1_00)
            val repository = FakeTransactionRepository(transactions = listOf(a))
            val vm = viewModel(repository)
            advanceUntilIdle()

            vm.delete("t1")
            advanceUntilIdle()
            assertEquals(0, (vm.uiState.value as LedgerUiState.Loaded).totalCount)
            assertEquals(listOf("t1"), repository.deletedIds)

            vm.undoDelete("t1")
            advanceUntilIdle()
            assertEquals(1, (vm.uiState.value as LedgerUiState.Loaded).totalCount)
            assertEquals(listOf("t1"), repository.restoredIds)
        }
}
