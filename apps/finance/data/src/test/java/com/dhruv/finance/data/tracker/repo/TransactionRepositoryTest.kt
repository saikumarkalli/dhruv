package com.dhruv.finance.data.tracker.repo

import com.dhruv.finance.data.tracker.dto.MonthSummaryDto
import com.dhruv.finance.data.tracker.dto.TransactionDto
import com.dhruv.finance.data.tracker.dto.TransactionEventDto
import com.dhruv.finance.data.tracker.dto.TransactionUpsertDto
import com.dhruv.finance.data.tracker.mapper.toDomain
import com.dhruv.finance.data.tracker.model.Transaction
import com.dhruv.finance.data.tracker.model.TransactionSource
import com.dhruv.finance.data.tracker.model.TransactionType
import com.dhruv.finance.data.tracker.net.MoneyApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.YearMonth

/**
 * A partial [MoneyApi] fake — every method not exercised by a given test throws, so a test that
 * hits an un-stubbed call fails loudly instead of returning a silent default.
 */
private open class FakeMoneyApi : MoneyApi by UnimplementedMoneyApi

private object UnimplementedMoneyApi : MoneyApi {
    private fun unimplemented(): Nothing = error("not stubbed for this test")

    override suspend fun listAccounts() = unimplemented()

    override suspend fun listAccountBalances() = unimplemented()

    override suspend fun createAccount(body: com.dhruv.finance.data.tracker.dto.AccountUpsertDto) = unimplemented()

    override suspend fun updateAccount(
        id: String,
        body: com.dhruv.finance.data.tracker.dto.AccountUpsertDto,
    ) = unimplemented()

    override suspend fun patchAccount(
        id: String,
        body: Map<String, Any?>,
    ) = unimplemented()

    override suspend fun listCategories() = unimplemented()

    override suspend fun listCategorySpend(month: String) = unimplemented()

    override suspend fun createCategory(body: com.dhruv.finance.data.tracker.dto.CategoryUpsertDto) = unimplemented()

    override suspend fun renameCategory(
        id: String,
        body: com.dhruv.finance.data.tracker.dto.CategoryRenameDto,
    ) = unimplemented()

    override suspend fun updateCategoryExcluded(
        id: String,
        body: Map<String, Boolean>,
    ) = unimplemented()

    override suspend fun softDeleteCategory(
        id: String,
        body: Map<String, String>,
    ) = unimplemented()

    override suspend fun mergeCategories(body: com.dhruv.finance.data.tracker.dto.MergeCategoriesRequestDto) = unimplemented()

    override suspend fun countTransactionsForCategory(categoryId: String) = unimplemented()

    override suspend fun listTransactions(
        occurredAtGte: String,
        occurredAtLt: String,
    ) = unimplemented()

    override suspend fun getTransaction(id: String) = unimplemented()

    override suspend fun listTransactionEvents(
        transactionId: String,
        order: String,
    ) = unimplemented()

    override suspend fun getMonthSummary(month: String) = unimplemented()

    override suspend fun createTransaction(body: TransactionUpsertDto) = unimplemented()

    override suspend fun updateTransaction(
        id: String,
        body: TransactionUpsertDto,
    ) = unimplemented()

    override suspend fun patchTransaction(
        id: String,
        body: Map<String, Any?>,
    ) = unimplemented()

    override suspend fun listRecurringTemplates() = unimplemented()

    override suspend fun createRecurringTemplate(body: com.dhruv.finance.data.tracker.dto.RecurringTemplateUpsertDto) =
        unimplemented()

    override suspend fun setRecurringPaused(
        id: String,
        body: com.dhruv.finance.data.tracker.dto.RecurringPauseDto,
    ) = unimplemented()

    override suspend fun advanceRecurringNextRun(
        id: String,
        body: Map<String, String>,
    ) = unimplemented()

    override suspend fun listPendingSuggestions() = unimplemented()

    override suspend fun createSuggestion(body: com.dhruv.finance.data.tracker.dto.SuggestionUpsertDto) = unimplemented()

    override suspend fun setSuggestionStatus(
        id: String,
        body: com.dhruv.finance.data.tracker.dto.SuggestionStatusDto,
    ) = unimplemented()
}

private fun expenseTransaction(
    id: String = "txn-1",
    amountPaise: Long = 50_00,
    accountId: String = "acc-1",
    categoryId: String? = "cat-1",
) = Transaction(
    id = id,
    type = TransactionType.EXPENSE,
    amountPaise = amountPaise,
    accountId = accountId,
    toAccountId = null,
    categoryId = categoryId,
    payee = "Coffee shop",
    note = null,
    occurredAt = Instant.parse("2026-09-01T10:00:00Z"),
    cleared = true,
    receiptPath = null,
    goalId = null,
    recurringId = null,
    splitGroupId = null,
    source = TransactionSource.MANUAL,
)

class TransactionRepositoryTest {
    // MNY-BR-001: SQL (v_month_summary/v_category_spend) is the enforcement authority for
    // "a TRANSFER contributes nothing to expense/category totals" (research R3, verified live at
    // the Sec step, not in a JVM test — Robolectric-SQLite is unreliable on this Windows dev
    // machine and tracker data has no local Room layer to test against anyway). What a JVM test
    // *can* prove is that the Kotlin mapping layer between that server response and the domain
    // model doesn't quietly re-introduce the excluded amount — a `MonthSummaryDto` with a large
    // `transfer_paise` must map to a `MonthSummary` whose `expensePaise` is untouched by it.
    @Test
    fun `mapping a month summary never folds transfer_paise into expense or income`() =
        runTest {
            val api =
                object : FakeMoneyApi() {
                    override suspend fun getMonthSummary(month: String) =
                        listOf(
                            MonthSummaryDto(
                                month = "2026-09-01",
                                incomePaise = 1_000_00,
                                expensePaise = 200_00,
                                excludedPaise = 0,
                                transferPaise = 5_000_00,
                            ),
                        )
                }
            val repo: TransactionRepository = TransactionRepositoryImpl(api)

            val summary = repo.monthSummary(YearMonth.of(2026, 9)).getOrThrow()

            assertEquals(200_00L, summary?.expensePaise)
            assertEquals(1_000_00L, summary?.incomePaise)
            assertEquals(5_000_00L, summary?.transferPaise)
        }

    // MNY-BR-001 (category-share half): a category-spend row's spend_paise/share_percent come
    // straight from finance.v_category_spend, which already excludes TRANSFER rows in SQL — the
    // Kotlin mapper must pass those numbers through unchanged, never recompute them.
    @Test
    fun `category spend mapping passes server-computed totals through unchanged`() =
        runTest {
            val api =
                object : FakeMoneyApi() {
                    override suspend fun listCategorySpend(month: String) =
                        listOf(
                            com.dhruv.finance.data.tracker.dto.CategorySpendDto(
                                month = "2026-09-01",
                                categoryId = "cat-1",
                                categoryName = "Groceries",
                                categoryKind = "EXPENSE",
                                excludedFromSpend = false,
                                spendPaise = 340_00,
                                sharePercent = 42.5,
                            ),
                        )
                    override suspend fun listCategories() =
                        listOf(
                            com.dhruv.finance.data.tracker.dto.CategoryDto(
                                id = "cat-1",
                                name = "Groceries",
                                kind = "EXPENSE",
                            ),
                        )
                }
            val spend = api.listCategorySpend("eq.2026-09-01").associateBy { it.categoryId }
            val categoryDto = api.listCategories().first()
            val category = categoryDto.toDomain(spend[categoryDto.id])

            assertEquals(340_00L, category.spendPaise)
            assertEquals(42.5, category.sharePercent)
        }

    // MNY-BR-006 companion (T022): the repository boundary rejects a shape the DB CHECK
    // (transactions_transfer_shape) would also reject, so the user sees a fast local error instead
    // of waiting on a round trip that fails with an opaque Postgres constraint violation.
    @Test
    fun `create rejects a TRANSFER with no destination account`() =
        runTest {
            val repo: TransactionRepository = TransactionRepositoryImpl(FakeMoneyApi())
            val transfer =
                expenseTransaction(categoryId = null).copy(type = TransactionType.TRANSFER, toAccountId = null)

            val result = repo.createTransaction(transfer)

            assertTrue(result.isFailure)
        }

    @Test
    fun `create rejects a TRANSFER whose destination equals its source account`() =
        runTest {
            val repo: TransactionRepository = TransactionRepositoryImpl(FakeMoneyApi())
            val transfer =
                expenseTransaction(accountId = "acc-1", categoryId = null)
                    .copy(type = TransactionType.TRANSFER, toAccountId = "acc-1")

            val result = repo.createTransaction(transfer)

            assertTrue(result.isFailure)
        }

    @Test
    fun `create rejects a TRANSFER that also carries a category`() =
        runTest {
            val repo: TransactionRepository = TransactionRepositoryImpl(FakeMoneyApi())
            val transfer =
                expenseTransaction(categoryId = "cat-1")
                    .copy(type = TransactionType.TRANSFER, toAccountId = "acc-2")

            val result = repo.createTransaction(transfer)

            assertTrue(result.isFailure)
        }

    @Test
    fun `create rejects an EXPENSE with no category`() =
        runTest {
            val repo: TransactionRepository = TransactionRepositoryImpl(FakeMoneyApi())
            val expense = expenseTransaction(categoryId = null)

            val result = repo.createTransaction(expense)

            assertTrue(result.isFailure)
        }

    @Test
    fun `create rejects an EXPENSE that also carries a destination account`() =
        runTest {
            val repo: TransactionRepository = TransactionRepositoryImpl(FakeMoneyApi())
            val expense = expenseTransaction().copy(toAccountId = "acc-2")

            val result = repo.createTransaction(expense)

            assertTrue(result.isFailure)
        }

    @Test
    fun `create accepts a well-formed EXPENSE and returns the mapped domain transaction`() =
        runTest {
            val dto =
                TransactionDto(
                    id = "txn-1",
                    type = "EXPENSE",
                    amountPaise = 50_00,
                    accountId = "acc-1",
                    categoryId = "cat-1",
                    occurredAt = "2026-09-01T10:00:00Z",
                )
            val api =
                object : FakeMoneyApi() {
                    override suspend fun createTransaction(body: TransactionUpsertDto) = listOf(dto)
                }
            val repo: TransactionRepository = TransactionRepositoryImpl(api)

            val result = repo.createTransaction(expenseTransaction())

            assertTrue(result.isSuccess)
            assertEquals("txn-1", result.getOrThrow().id)
            assertFalse(result.getOrThrow().type == TransactionType.TRANSFER)
        }

    @Test
    fun `create accepts a well-formed TRANSFER`() =
        runTest {
            val dto =
                TransactionDto(
                    id = "txn-2",
                    type = "TRANSFER",
                    amountPaise = 100_00,
                    accountId = "acc-1",
                    toAccountId = "acc-2",
                    categoryId = null,
                    occurredAt = "2026-09-01T10:00:00Z",
                )
            val api =
                object : FakeMoneyApi() {
                    override suspend fun createTransaction(body: TransactionUpsertDto) = listOf(dto)
                }
            val repo: TransactionRepository = TransactionRepositoryImpl(api)
            val transfer =
                expenseTransaction(categoryId = null).copy(type = TransactionType.TRANSFER, toAccountId = "acc-2")

            val result = repo.createTransaction(transfer)

            assertTrue(result.isSuccess)
            assertEquals("txn-2", result.getOrThrow().id)
        }
}
