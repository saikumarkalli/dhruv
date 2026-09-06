package com.dhruv.finance.data.tracker.repo

import com.dhruv.finance.data.tracker.mapper.toDomain
import com.dhruv.finance.data.tracker.mapper.toUpsertDto
import com.dhruv.finance.data.tracker.model.MonthSummary
import com.dhruv.finance.data.tracker.model.Transaction
import com.dhruv.finance.data.tracker.model.TransactionEvent
import com.dhruv.finance.data.tracker.net.MoneyApi
import com.dhruv.finance.data.tracker.net.SupabaseClientFactory
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.UUID

/** A pre-guessed category/account pair for quick-add (FR-002/T030) — last-used for this payee,
 * falling back to the account/category used most often overall. Either side may be null if there
 * is no history yet to guess from. */
data class TransactionGuess(
    val accountId: String?,
    val categoryId: String?,
)

/**
 * Money tab transactions (D1-D4, US1). Splits (FR-004) are sibling rows sharing
 * [Transaction.splitGroupId] — there is no parent row holding a total (data-model.md); callers
 * write each part as its own [createTransaction] call with the same `splitGroupId`.
 */
interface TransactionRepository {
    suspend fun listForMonth(month: YearMonth): Result<List<Transaction>>

    suspend fun monthSummary(month: YearMonth): Result<MonthSummary?>

    suspend fun createTransaction(
        transaction: Transaction,
        requestId: String = UUID.randomUUID().toString(),
    ): Result<Transaction>

    suspend fun updateTransaction(transaction: Transaction): Result<Transaction>

    suspend fun softDeleteTransaction(transactionId: String): Result<Unit>

    /** FR-006's undo (DESIGN-SYSTEM §8 — soft-delete + `UndoSnackbarHost` + a recoverable
     * location). Clears `deleted_at` on the same row rather than recreating it, so the
     * transaction's id, `transaction_events` history and any `split_group_id` all survive. */
    suspend fun restoreTransaction(transactionId: String): Result<Unit>

    suspend fun getTransaction(transactionId: String): Result<Transaction?>

    suspend fun listEvents(transactionId: String): Result<List<TransactionEvent>>

    /** FR-002/T030: last-used account/category for [payee], falling back to whichever is used
     * most often across this user's recent transactions. Read-only, best-effort — a failure here
     * must never block quick-add, so callers should treat [Result.failure] as "no guess". */
    suspend fun guessFor(payee: String?): Result<TransactionGuess>
}

class TransactionRepositoryImpl(
    private val api: MoneyApi,
) : TransactionRepository {
    constructor(supabaseClientFactory: SupabaseClientFactory) : this(
        supabaseClientFactory.dataRetrofit.create(MoneyApi::class.java),
    )

    @Suppress("TooGenericExceptionCaught")
    override suspend fun listForMonth(month: YearMonth): Result<List<Transaction>> =
        try {
            val start = month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant()
            val end =
                month
                    .plusMonths(1)
                    .atDay(1)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
            val rows = api.listTransactions("gte.$start", "lt.$end")
            Result.success(rows.map { it.toDomain() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun monthSummary(month: YearMonth): Result<MonthSummary?> =
        try {
            val monthStart = month.atDay(1).toString()
            val row = api.getMonthSummary("eq.$monthStart").firstOrNull()
            Result.success(row?.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun createTransaction(
        transaction: Transaction,
        requestId: String,
    ): Result<Transaction> {
        validateShape(transaction)?.let { return Result.failure(it) }
        return try {
            val created = api.createTransaction(transaction.toUpsertDto(requestId)).first()
            Result.success(created.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun updateTransaction(transaction: Transaction): Result<Transaction> {
        validateShape(transaction)?.let { return Result.failure(it) }
        return try {
            val updated = api.updateTransaction("eq.${transaction.id}", transaction.toUpsertDto()).first()
            Result.success(updated.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun softDeleteTransaction(transactionId: String): Result<Unit> =
        try {
            api.patchTransaction("eq.$transactionId", mapOf("deleted_at" to Instant.now().toString()))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun restoreTransaction(transactionId: String): Result<Unit> =
        try {
            api.patchTransaction("eq.$transactionId", mapOf("deleted_at" to null))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun getTransaction(transactionId: String): Result<Transaction?> =
        try {
            Result.success(api.getTransaction("eq.$transactionId").firstOrNull()?.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun listEvents(transactionId: String): Result<List<TransactionEvent>> =
        try {
            Result.success(api.listTransactionEvents("eq.$transactionId").map { it.toDomain() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun guessFor(payee: String?): Result<TransactionGuess> =
        try {
            // Best-effort, capped read: the last 50 transactions is enough signal for a pre-guess
            // and avoids scanning a user's whole history on every quick-add open.
            val recent =
                api
                    .listTransactions(
                        "gte.${Instant.EPOCH}",
                        "lt.${Instant.now().plusSeconds(1)}",
                    ).map { it.toDomain() }
                    .filter { it.type != com.dhruv.finance.data.tracker.model.TransactionType.TRANSFER }

            val byPayee = if (payee.isNullOrBlank()) emptyList() else recent.filter { it.payee == payee }
            val source = byPayee.ifEmpty { recent }

            val account =
                source
                    .groupingBy { it.accountId }
                    .eachCount()
                    .maxByOrNull { it.value }
                    ?.key
            val category =
                source
                    .mapNotNull { it.categoryId }
                    .groupingBy { it }
                    .eachCount()
                    .maxByOrNull { it.value }
                    ?.key

            Result.success(TransactionGuess(accountId = account, categoryId = category))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    /** Mirrors the DB's `transactions_transfer_shape` CHECK (T022) — a fast local rejection
     * instead of a round trip that fails with an opaque Postgres constraint violation. Returns
     * `null` when the shape is valid. */
    private fun validateShape(transaction: Transaction): IllegalArgumentException? =
        when (transaction.type) {
            com.dhruv.finance.data.tracker.model.TransactionType.TRANSFER ->
                when {
                    transaction.toAccountId == null ->
                        IllegalArgumentException("a TRANSFER requires a destination account")
                    transaction.toAccountId == transaction.accountId ->
                        IllegalArgumentException("a TRANSFER's destination must differ from its source account")
                    transaction.categoryId != null ->
                        IllegalArgumentException("a TRANSFER may not carry a category")
                    else -> null
                }
            com.dhruv.finance.data.tracker.model.TransactionType.EXPENSE,
            com.dhruv.finance.data.tracker.model.TransactionType.INCOME,
            ->
                when {
                    transaction.toAccountId != null ->
                        IllegalArgumentException("${transaction.type} may not carry a destination account")
                    transaction.categoryId == null ->
                        IllegalArgumentException("${transaction.type} requires a category")
                    else -> null
                }
        }
}
