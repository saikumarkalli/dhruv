package com.dhruv.finance.data.tracker.repo

import com.dhruv.finance.data.tracker.dto.RecurringPauseDto
import com.dhruv.finance.data.tracker.dto.RecurringTemplateUpsertDto
import com.dhruv.finance.data.tracker.mapper.toDomain
import com.dhruv.finance.data.tracker.model.RecurringTemplate
import com.dhruv.finance.data.tracker.model.Transaction
import com.dhruv.finance.data.tracker.net.MoneyApi
import com.dhruv.finance.data.tracker.net.SupabaseClientFactory
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** The transaction-shaped fields a [RecurringTemplate.template] JSON blob carries (data-model.md:
 * "the transaction shape to produce — type, amount, category, account, payee"). Kept as a plain
 * key set (not a `@JsonClass`) since the DTO field is already typed `Map<String, Any?>` end to
 * end — see [RecurringTemplateDto]/[SuggestionDto]. */
object RecurringTemplateKeys {
    const val TYPE = "type"
    const val AMOUNT_PAISE = "amountPaise"
    const val ACCOUNT_ID = "accountId"
    const val CATEGORY_ID = "categoryId"
    const val PAYEE = "payee"
    const val NOTE = "note"
}

/** Every `FREQ=` value this minimal RRULE reader understands — no INTERVAL/BYDAY/COUNT/UNTIL
 * (research R7 scopes this phase to "the smallest mechanism that satisfies BR-D4"; a fuller RRULE
 * parser is a follow-up, not silently assumed to already exist). */
private fun advance(
    date: LocalDate,
    rrule: String,
): LocalDate =
    when {
        rrule.contains("FREQ=DAILY") -> date.plusDays(1)
        rrule.contains("FREQ=WEEKLY") -> date.plusWeeks(1)
        rrule.contains("FREQ=YEARLY") -> date.plusYears(1)
        else -> date.plusMonths(1) // FREQ=MONTHLY, and the default for an unrecognised rule
    }

/**
 * Money tab recurring definitions (D9, US6). No server scheduler exists (research R7) — due
 * occurrences are materialised client-side into [SuggestionRepository] rows on open, idempotently
 * (the DB's unique `(recurring_id, due_on)` constraint, surfaced here via `Prefer:
 * resolution=ignore-duplicates` on the suggestions insert).
 */
interface RecurringRepository {
    suspend fun listActive(): Result<List<RecurringTemplate>>

    /** Creates a recurring definition from an existing transaction's shape (FR-027, "make it
     * recurring" on D3/D4) — writes only the `recurring_templates` row, never an immediate
     * duplicate transaction. */
    suspend fun createFromTransaction(
        transaction: Transaction,
        rrule: String,
        nextRun: LocalDate,
        amountIsVariable: Boolean = false,
    ): Result<RecurringTemplate>

    suspend fun pause(templateId: String): Result<Unit>

    suspend fun resume(templateId: String): Result<Unit>

    /** For every active, non-paused template whose `nextRun` has arrived, writes one pending
     * [com.dhruv.finance.data.tracker.model.PendingEntry] (never a ledger transaction, FR-028) and
     * advances `nextRun` — safe to call on every app/tab open (idempotent). */
    suspend fun materialiseDue(
        suggestionRepository: SuggestionRepository,
        today: LocalDate = LocalDate.now(),
    ): Result<Unit>
}

class RecurringRepositoryImpl(
    private val api: MoneyApi,
) : RecurringRepository {
    constructor(supabaseClientFactory: SupabaseClientFactory) : this(
        supabaseClientFactory.dataRetrofit.create(MoneyApi::class.java),
    )

    @Suppress("TooGenericExceptionCaught")
    override suspend fun listActive(): Result<List<RecurringTemplate>> =
        try {
            Result.success(api.listRecurringTemplates().map { it.toDomain() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun createFromTransaction(
        transaction: Transaction,
        rrule: String,
        nextRun: LocalDate,
        amountIsVariable: Boolean,
    ): Result<RecurringTemplate> =
        try {
            val template =
                mapOf(
                    RecurringTemplateKeys.TYPE to transaction.type.name,
                    RecurringTemplateKeys.AMOUNT_PAISE to transaction.amountPaise,
                    RecurringTemplateKeys.ACCOUNT_ID to transaction.accountId,
                    RecurringTemplateKeys.CATEGORY_ID to transaction.categoryId,
                    RecurringTemplateKeys.PAYEE to transaction.payee,
                    RecurringTemplateKeys.NOTE to transaction.note,
                )
            val created =
                api
                    .createRecurringTemplate(
                        RecurringTemplateUpsertDto(
                            template = template,
                            rrule = rrule,
                            nextRun = nextRun.toString(),
                            amountIsVariable = amountIsVariable,
                            requestId = UUID.randomUUID().toString(),
                        ),
                    ).first()
            Result.success(created.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun pause(templateId: String): Result<Unit> =
        try {
            api.setRecurringPaused("eq.$templateId", RecurringPauseDto(paused = true, pausedAt = Instant.now().toString()))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun resume(templateId: String): Result<Unit> =
        try {
            api.setRecurringPaused("eq.$templateId", RecurringPauseDto(paused = false, pausedAt = null))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun materialiseDue(
        suggestionRepository: SuggestionRepository,
        today: LocalDate,
    ): Result<Unit> =
        try {
            val due = api.listRecurringTemplates().map { it.toDomain() }.filter { !it.paused && !it.nextRun.isAfter(today) }
            for (template in due) {
                suggestionRepository.createFromRecurring(template).getOrElse { return Result.failure(it) }
                api.advanceRecurringNextRun("eq.${template.id}", mapOf("next_run" to advance(template.nextRun, template.rrule).toString()))
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
