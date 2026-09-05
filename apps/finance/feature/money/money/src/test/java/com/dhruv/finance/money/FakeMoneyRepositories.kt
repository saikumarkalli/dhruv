package com.dhruv.finance.money

import com.dhruv.finance.data.tracker.model.Account
import com.dhruv.finance.data.tracker.model.Category
import com.dhruv.finance.data.tracker.model.MonthSummary
import com.dhruv.finance.data.tracker.model.Transaction
import com.dhruv.finance.data.tracker.model.TransactionEvent
import com.dhruv.finance.data.tracker.repo.AccountRepository
import com.dhruv.finance.data.tracker.repo.CategoryRepository
import com.dhruv.finance.data.tracker.repo.TransactionGuess
import com.dhruv.finance.data.tracker.repo.TransactionRepository
import java.time.YearMonth
import java.util.UUID

/** Shared test fakes for the Money feature module's ViewModel tests — in-memory, no network
 * (research R10: fakes at the HTTP boundary; these sit one layer above, at the repository
 * interface, since the ViewModels under test never see [com.dhruv.finance.data.tracker.net.MoneyApi]
 * directly). */
class FakeAccountRepository(
    private var accounts: List<Account> = emptyList(),
) : AccountRepository {
    override suspend fun listAccounts(): Result<List<Account>> = Result.success(accounts)

    override suspend fun createAccount(account: Account): Result<Account> {
        val created = account.copy(id = UUID.randomUUID().toString())
        accounts = accounts + created
        return Result.success(created)
    }

    override suspend fun updateAccount(account: Account): Result<Account> = Result.success(account)

    override suspend fun softDeleteAccount(accountId: String): Result<Unit> = Result.success(Unit)

    override suspend fun markReconciled(accountId: String): Result<Unit> = Result.success(Unit)
}

class FakeCategoryRepository(
    private var categories: List<Category> = emptyList(),
    /** Per-category exact transaction count, keyed by id — backs [countTransactionsForCategory],
     * the same call the D8 merge confirmation and the Uncategorised row use. */
    private val transactionCounts: Map<String, Int> = emptyMap(),
    private val mergeResult: Int = 0,
) : CategoryRepository {
    val ensureReservedCalls = mutableListOf<Unit>()
    val mergeCalls = mutableListOf<Pair<String, String>>()

    override suspend fun listCategories(): Result<List<Category>> = Result.success(categories)

    override suspend fun listCategoriesWithSpend(month: YearMonth): Result<List<Category>> = Result.success(categories)

    override suspend fun createCategory(category: Category): Result<Category> {
        val created = category.copy(id = UUID.randomUUID().toString())
        categories = categories + created
        return Result.success(created)
    }

    override suspend fun renameCategory(
        categoryId: String,
        newName: String,
    ): Result<Category> {
        val renamed = categories.first { it.id == categoryId }.copy(name = newName)
        categories = categories.map { if (it.id == categoryId) renamed else it }
        return Result.success(renamed)
    }

    override suspend fun setExcludedFromSpend(
        categoryId: String,
        excluded: Boolean,
    ): Result<Category> {
        val updated = categories.first { it.id == categoryId }.copy(excludedFromSpend = excluded)
        categories = categories.map { if (it.id == categoryId) updated else it }
        return Result.success(updated)
    }

    override suspend fun mergeCategories(
        sourceId: String,
        targetId: String,
    ): Result<Int> {
        mergeCalls += sourceId to targetId
        return Result.success(mergeResult)
    }

    override suspend fun softDeleteCategory(categoryId: String): Result<Unit> = Result.success(Unit)

    override suspend fun ensureReservedCategories(): Result<Unit> {
        ensureReservedCalls += Unit
        return Result.success(Unit)
    }

    override suspend fun countTransactionsForCategory(categoryId: String): Result<Int> =
        Result.success(transactionCounts[categoryId] ?: 0)
}

class FakeTransactionRepository(
    private var transactions: List<Transaction> = emptyList(),
    private val guess: TransactionGuess = TransactionGuess(accountId = null, categoryId = null),
    private val createResult: (Transaction) -> Result<Transaction> = { txn ->
        Result.success(txn.copy(id = UUID.randomUUID().toString()))
    },
) : TransactionRepository {
    val created = mutableListOf<Transaction>()

    override suspend fun listForMonth(month: YearMonth): Result<List<Transaction>> = Result.success(transactions)

    override suspend fun monthSummary(month: YearMonth): Result<MonthSummary?> = Result.success(null)

    override suspend fun createTransaction(
        transaction: Transaction,
        requestId: String,
    ): Result<Transaction> =
        createResult(transaction).onSuccess {
            created += it
            transactions = transactions + it
        }

    override suspend fun updateTransaction(transaction: Transaction): Result<Transaction> = Result.success(transaction)

    override suspend fun softDeleteTransaction(transactionId: String): Result<Unit> = Result.success(Unit)

    override suspend fun getTransaction(transactionId: String): Result<Transaction?> =
        Result.success(transactions.firstOrNull { it.id == transactionId })

    override suspend fun listEvents(transactionId: String): Result<List<TransactionEvent>> = Result.success(emptyList())

    override suspend fun guessFor(payee: String?): Result<TransactionGuess> = Result.success(guess)
}
