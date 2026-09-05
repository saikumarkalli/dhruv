package com.dhruv.finance.data.tracker.repo

import com.dhruv.finance.data.tracker.dto.SuggestionStatusDto
import com.dhruv.finance.data.tracker.dto.SuggestionUpsertDto
import com.dhruv.finance.data.tracker.mapper.toDomain
import com.dhruv.finance.data.tracker.model.PendingEntry
import com.dhruv.finance.data.tracker.model.RecurringTemplate
import com.dhruv.finance.data.tracker.model.Transaction
import com.dhruv.finance.data.tracker.model.TransactionSource
import com.dhruv.finance.data.tracker.model.TransactionType
import com.dhruv.finance.data.tracker.net.MoneyApi
import com.dhruv.finance.data.tracker.net.SupabaseClientFactory
import kotlinx.coroutines.CancellationException
import java.time.Instant

/**
 * Money tab pending entries (D9-review, US6) — a proposed transaction awaiting the user's
 * accept/dismiss (FR-029). Never part of any total until [accept] writes the real transaction.
 */
interface SuggestionRepository {
    suspend fun listPending(): Result<List<PendingEntry>>

    /** Materialise-on-open's write path (research R7) — the DB's unique `(recurring_id, due_on)`
     * constraint plus `Prefer: resolution=ignore-duplicates` makes this safe to call repeatedly
     * for the same due occurrence without duplicating a pending entry. */
    suspend fun createFromRecurring(template: RecurringTemplate): Result<Unit>

    /** Writes the real transaction (`source = RECURRING`, `recurring_id` set — the DB trigger
     * emits `ACCEPTED_FROM_RECURRING` for it, FR-029) and marks the suggestion `ACCEPTED`.
     * Dismissing (see [dismiss]) writes nothing. */
    suspend fun accept(entry: PendingEntry): Result<Transaction>

    suspend fun dismiss(entryId: String): Result<Unit>
}

class SuggestionRepositoryImpl(
    private val api: MoneyApi,
    private val transactionRepository: TransactionRepository,
) : SuggestionRepository {
    constructor(
        supabaseClientFactory: SupabaseClientFactory,
        transactionRepository: TransactionRepository,
    ) : this(supabaseClientFactory.dataRetrofit.create(MoneyApi::class.java), transactionRepository)

    @Suppress("TooGenericExceptionCaught")
    override suspend fun listPending(): Result<List<PendingEntry>> =
        try {
            Result.success(api.listPendingSuggestions().map { it.toDomain() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun createFromRecurring(template: RecurringTemplate): Result<Unit> =
        try {
            api.createSuggestion(
                SuggestionUpsertDto(recurringId = template.id, dueOn = template.nextRun.toString(), parsed = template.template),
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun accept(entry: PendingEntry): Result<Transaction> =
        try {
            val parsed = entry.parsed
            val transaction =
                Transaction(
                    id = "",
                    type = TransactionType.valueOf(parsed[RecurringTemplateKeys.TYPE] as String),
                    amountPaise = (parsed[RecurringTemplateKeys.AMOUNT_PAISE] as Number).toLong(),
                    accountId = parsed[RecurringTemplateKeys.ACCOUNT_ID] as String,
                    toAccountId = null,
                    categoryId = parsed[RecurringTemplateKeys.CATEGORY_ID] as String?,
                    payee = parsed[RecurringTemplateKeys.PAYEE] as String?,
                    note = parsed[RecurringTemplateKeys.NOTE] as String?,
                    occurredAt = Instant.now(),
                    cleared = true,
                    receiptPath = null,
                    goalId = null,
                    recurringId = entry.recurringId,
                    splitGroupId = null,
                    source = TransactionSource.RECURRING,
                )
            val created = transactionRepository.createTransaction(transaction).getOrElse { return Result.failure(it) }
            api.setSuggestionStatus("eq.${entry.id}", SuggestionStatusDto(status = "ACCEPTED"))
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun dismiss(entryId: String): Result<Unit> =
        try {
            api.setSuggestionStatus("eq.$entryId", SuggestionStatusDto(status = "IGNORED"))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
