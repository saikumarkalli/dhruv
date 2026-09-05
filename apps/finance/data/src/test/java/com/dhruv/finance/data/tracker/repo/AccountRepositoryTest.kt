package com.dhruv.finance.data.tracker.repo

import com.dhruv.finance.data.tracker.dto.AccountBalanceDto
import com.dhruv.finance.data.tracker.dto.AccountDto
import com.dhruv.finance.data.tracker.dto.AccountUpsertDto
import com.dhruv.finance.data.tracker.dto.CategoryRenameDto
import com.dhruv.finance.data.tracker.dto.CategoryUpsertDto
import com.dhruv.finance.data.tracker.dto.MergeCategoriesRequestDto
import com.dhruv.finance.data.tracker.dto.RecurringPauseDto
import com.dhruv.finance.data.tracker.dto.RecurringTemplateUpsertDto
import com.dhruv.finance.data.tracker.dto.SuggestionStatusDto
import com.dhruv.finance.data.tracker.dto.SuggestionUpsertDto
import com.dhruv.finance.data.tracker.dto.TransactionUpsertDto
import com.dhruv.finance.data.tracker.model.Category
import com.dhruv.finance.data.tracker.model.MonthSummary
import com.dhruv.finance.data.tracker.model.Transaction
import com.dhruv.finance.data.tracker.model.TransactionEvent
import com.dhruv.finance.data.tracker.model.TransactionSource
import com.dhruv.finance.data.tracker.model.TransactionType
import com.dhruv.finance.data.tracker.net.MoneyApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

/**
 * A partial [MoneyApi] fake — every method not exercised by a given test throws, so a test that
 * hits an un-stubbed call fails loudly instead of returning a silent default. (Mirrors
 * `TransactionRepositoryTest`'s own fake — not shared across files by this repo's convention.)
 */
private open class AccountFakeMoneyApi : MoneyApi by AccountUnimplementedMoneyApi

private object AccountUnimplementedMoneyApi : MoneyApi {
    private fun unimplemented(): Nothing = error("not stubbed for this test")

    override suspend fun listAccounts() = unimplemented()

    override suspend fun listAccountBalances() = unimplemented()

    override suspend fun createAccount(body: AccountUpsertDto) = unimplemented()

    override suspend fun updateAccount(
        id: String,
        body: AccountUpsertDto,
    ) = unimplemented()

    override suspend fun patchAccount(
        id: String,
        body: Map<String, Any?>,
    ) = unimplemented()

    override suspend fun listCategories() = unimplemented()

    override suspend fun listCategorySpend(month: String) = unimplemented()

    override suspend fun createCategory(body: CategoryUpsertDto) = unimplemented()

    override suspend fun renameCategory(
        id: String,
        body: CategoryRenameDto,
    ) = unimplemented()

    override suspend fun updateCategoryExcluded(
        id: String,
        body: Map<String, Boolean>,
    ) = unimplemented()

    override suspend fun softDeleteCategory(
        id: String,
        body: Map<String, String>,
    ) = unimplemented()

    override suspend fun mergeCategories(body: MergeCategoriesRequestDto) = unimplemented()

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

    override suspend fun createRecurringTemplate(body: RecurringTemplateUpsertDto) = unimplemented()

    override suspend fun setRecurringPaused(
        id: String,
        body: RecurringPauseDto,
    ) = unimplemented()

    override suspend fun advanceRecurringNextRun(
        id: String,
        body: Map<String, String>,
    ) = unimplemented()

    override suspend fun listPendingSuggestions() = unimplemented()

    override suspend fun createSuggestion(body: SuggestionUpsertDto) = unimplemented()

    override suspend fun setSuggestionStatus(
        id: String,
        body: SuggestionStatusDto,
    ) = unimplemented()
}

/** Delegates every write straight through so [AccountRepositoryImpl.reconcileAccount] can be
 * exercised without a real network — records every [createTransaction] call so a test can assert
 * the adjustment's shape (MNY-BR-002/FR-021). */
private class RecordingTransactionRepository : TransactionRepository {
    val createdTransactions = mutableListOf<Transaction>()

    override suspend fun listForMonth(month: YearMonth): Result<List<Transaction>> = Result.success(emptyList())

    override suspend fun monthSummary(month: YearMonth): Result<MonthSummary?> = Result.success(null)

    override suspend fun createTransaction(
        transaction: Transaction,
        requestId: String,
    ): Result<Transaction> {
        createdTransactions += transaction
        return Result.success(transaction.copy(id = "adj-1"))
    }

    override suspend fun updateTransaction(transaction: Transaction): Result<Transaction> = Result.success(transaction)

    override suspend fun softDeleteTransaction(transactionId: String): Result<Unit> = Result.success(Unit)

    override suspend fun getTransaction(transactionId: String): Result<Transaction?> = Result.success(null)

    override suspend fun listEvents(transactionId: String): Result<List<TransactionEvent>> = Result.success(emptyList())

    override suspend fun guessFor(payee: String?): Result<TransactionGuess> =
        Result.success(TransactionGuess(accountId = null, categoryId = null))
}

/** A [CategoryRepository] whose reserved Adjustment category already exists — reconciliation
 * (FR-021) resolves the category id through this collaborator rather than hardcoding one. */
private class ReservedCategoryRepository(
    private val adjustmentCategoryId: String = "cat-adjustment",
) : CategoryRepository {
    override suspend fun listCategories(): Result<List<Category>> =
        Result.success(
            listOf(
                Category(
                    id = adjustmentCategoryId,
                    name = Category.RESERVED_ADJUSTMENT,
                    kind = com.dhruv.finance.data.tracker.model.CategoryKind.EXPENSE,
                    parentId = null,
                    icon = null,
                    excludedFromSpend = true,
                ),
            ),
        )

    override suspend fun createCategory(category: Category): Result<Category> = Result.success(category)

    override suspend fun renameCategory(
        categoryId: String,
        newName: String,
    ): Result<Category> = error("not stubbed for this test")

    override suspend fun setExcludedFromSpend(
        categoryId: String,
        excluded: Boolean,
    ): Result<Category> = error("not stubbed for this test")

    override suspend fun mergeCategories(
        sourceId: String,
        targetId: String,
    ): Result<Int> = error("not stubbed for this test")

    override suspend fun softDeleteCategory(categoryId: String): Result<Unit> = error("not stubbed for this test")

    override suspend fun ensureReservedCategories(): Result<Unit> = Result.success(Unit)

    override suspend fun listCategoriesWithSpend(month: java.time.YearMonth): Result<List<Category>> =
        error("not stubbed for this test")

    override suspend fun countTransactionsForCategory(categoryId: String): Result<Int> = error("not stubbed for this test")
}

private fun accountDto(
    id: String,
    type: String,
) = AccountDto(
    id = id,
    name = "$type account",
    type = type,
    openingBalancePaise = 0,
)

class AccountRepositoryTest {
    // MNY-BR-002 (FR-017): "spendable now" sums BANK/CASH/WALLET balances only — a credit card's
    // balance is money owed, not held, and must be excluded. The arithmetic sum itself is a
    // client-side filtered sum over a handful of rows (data-model.md), but the *classification*
    // (`countsAsSpendable`) must come from the domain model, never re-derived ad hoc by a screen.
    @Test
    fun `listAccounts maps bank cash and wallet as spendable and excludes credit`() =
        runTest {
            val api =
                object : AccountFakeMoneyApi() {
                    override suspend fun listAccounts() =
                        listOf(
                            accountDto("acc-bank", "BANK"),
                            accountDto("acc-cash", "CASH"),
                            accountDto("acc-wallet", "WALLET"),
                            accountDto("acc-credit", "CREDIT_CARD"),
                        )

                    override suspend fun listAccountBalances() =
                        listOf(
                            AccountBalanceDto("acc-bank", "BANK", true, 10_000_00),
                            AccountBalanceDto("acc-cash", "CASH", true, 2_000_00),
                            AccountBalanceDto("acc-wallet", "WALLET", true, 500_00),
                            AccountBalanceDto("acc-credit", "CREDIT_CARD", false, -3_000_00),
                        )
                }
            val repo: AccountRepository =
                AccountRepositoryImpl(api, RecordingTransactionRepository(), ReservedCategoryRepository())

            val accounts = repo.listAccounts().getOrThrow()

            val spendableNowPaise = accounts.filter { it.countsAsSpendable }.sumOf { it.balancePaise ?: 0L }
            assertEquals(12_500_00L, spendableNowPaise)

            val credit = accounts.first { it.id == "acc-credit" }
            assertFalse(credit.countsAsSpendable)
            assertTrue((credit.balancePaise ?: 0L) < 0L)
        }

    // FR-021/research R8: reconciling with no stated difference still clears staleness
    // (`reconciled_at` is set) but writes no adjustment transaction — nothing to explain.
    @Test
    fun `reconcile with no difference sets reconciled_at and writes no adjustment`() =
        runTest {
            var patchedFields: Map<String, Any?>? = null
            val api =
                object : AccountFakeMoneyApi() {
                    override suspend fun listAccountBalances() = listOf(AccountBalanceDto("acc-1", "BANK", true, 5_000_00))

                    override suspend fun patchAccount(
                        id: String,
                        body: Map<String, Any?>,
                    ): List<AccountDto> {
                        patchedFields = body
                        return listOf(accountDto("acc-1", "BANK"))
                    }
                }
            val transactionRepository = RecordingTransactionRepository()
            val repo: AccountRepository = AccountRepositoryImpl(api, transactionRepository, ReservedCategoryRepository())

            val result = repo.reconcileAccount("acc-1", statedBalancePaise = 5_000_00)

            assertTrue(result.isSuccess)
            assertTrue(transactionRepository.createdTransactions.isEmpty())
            assertNotNull(patchedFields?.get("reconciled_at"))
        }

    // FR-021/research R8: a stated balance HIGHER than computed writes an INCOME adjustment
    // (the account turns out to hold more than the ledger thought).
    @Test
    fun `reconcile with a higher stated balance writes an INCOME adjustment in the Adjustment category`() =
        runTest {
            val api =
                object : AccountFakeMoneyApi() {
                    override suspend fun listAccountBalances() = listOf(AccountBalanceDto("acc-1", "BANK", true, 5_000_00))

                    override suspend fun patchAccount(
                        id: String,
                        body: Map<String, Any?>,
                    ) = listOf(accountDto("acc-1", "BANK"))
                }
            val transactionRepository = RecordingTransactionRepository()
            val repo: AccountRepository = AccountRepositoryImpl(api, transactionRepository, ReservedCategoryRepository())

            val result = repo.reconcileAccount("acc-1", statedBalancePaise = 5_200_00)

            assertTrue(result.isSuccess)
            val adjustment = transactionRepository.createdTransactions.single()
            assertEquals(TransactionType.INCOME, adjustment.type)
            assertEquals(200_00L, adjustment.amountPaise)
            assertEquals("cat-adjustment", adjustment.categoryId)
            assertEquals(TransactionSource.RECONCILE, adjustment.source)
            assertEquals("acc-1", adjustment.accountId)
        }

    // Mirror case: a stated balance LOWER than computed writes an EXPENSE adjustment.
    @Test
    fun `reconcile with a lower stated balance writes an EXPENSE adjustment`() =
        runTest {
            val api =
                object : AccountFakeMoneyApi() {
                    override suspend fun listAccountBalances() = listOf(AccountBalanceDto("acc-1", "BANK", true, 5_000_00))

                    override suspend fun patchAccount(
                        id: String,
                        body: Map<String, Any?>,
                    ) = listOf(accountDto("acc-1", "BANK"))
                }
            val transactionRepository = RecordingTransactionRepository()
            val repo: AccountRepository = AccountRepositoryImpl(api, transactionRepository, ReservedCategoryRepository())

            val result = repo.reconcileAccount("acc-1", statedBalancePaise = 4_700_00)

            assertTrue(result.isSuccess)
            val adjustment = transactionRepository.createdTransactions.single()
            assertEquals(TransactionType.EXPENSE, adjustment.type)
            assertEquals(300_00L, adjustment.amountPaise)
            assertEquals(TransactionSource.RECONCILE, adjustment.source)
        }

    // FR-021: never edits opening_balance_paise — the reconcile PATCH body only ever carries
    // reconciled_at, whether or not an adjustment was written alongside it.
    @Test
    fun `reconcile never patches opening_balance_paise`() =
        runTest {
            var patchedFields: Map<String, Any?>? = null
            val api =
                object : AccountFakeMoneyApi() {
                    override suspend fun listAccountBalances() = listOf(AccountBalanceDto("acc-1", "BANK", true, 5_000_00))

                    override suspend fun patchAccount(
                        id: String,
                        body: Map<String, Any?>,
                    ): List<AccountDto> {
                        patchedFields = body
                        return listOf(accountDto("acc-1", "BANK"))
                    }
                }
            val repo: AccountRepository =
                AccountRepositoryImpl(api, RecordingTransactionRepository(), ReservedCategoryRepository())

            repo.reconcileAccount("acc-1", statedBalancePaise = 6_000_00)

            assertNull(patchedFields?.get("opening_balance_paise"))
        }
}
