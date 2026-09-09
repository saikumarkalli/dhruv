package com.dhruv.finance.data.tracker.repo

import com.dhruv.finance.data.tracker.dto.TransactionEventDto
import com.dhruv.finance.data.tracker.model.TransactionEventKind
import com.dhruv.finance.data.tracker.net.MoneyApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * T048/MNY-BR-006 — "every mutation appends exactly one `transaction_events` row" is a Postgres
 * trigger guarantee (`finance.fn_transaction_audit`, `supabase/schemas/finance/30_functions/
 * fn_transaction_audit.sql`): AFTER INSERT OR UPDATE dispatches on `NEW.source`/changed columns to
 * write CREATED/EDITED/CATEGORY_CHANGED/DELETED/ACCEPTED_FROM_RECURRING/RECONCILED. That is a live
 * database behaviour — it needs a real Postgres instance to fire, and this environment has no
 * Docker/Supabase CLI available to run one (research.md R10, the same constraint
 * `TransactionRepositoryTest`'s own top comment already documents for MNY-BR-001). A JVM test that
 * asserted "the trigger fires" would therefore be dishonest coverage — it would prove nothing about
 * the trigger and everything about a hand-written stub standing in for it.
 *
 * What this test actually proves is what `TransactionRepository.listEvents()` — the only Kotlin
 * code sitting between that trigger's output and the UI — is responsible for: that every
 * [TransactionEventDto] row the server returns, for each of the six [TransactionEventKind] values
 * the trigger can produce, is mapped into the correct domain
 * [com.dhruv.finance.data.tracker.model.TransactionEvent] without dropping or reordering rows. The
 * DB-level "exactly one row per mutation" claim itself is verified live against a real Supabase
 * project at the Sec step, not here.
 */
private open class FakeAuditMoneyApi : MoneyApi by UnimplementedAuditMoneyApi

private object UnimplementedAuditMoneyApi : MoneyApi {
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

    override suspend fun countTransactionsForAccount(accountId: String) = unimplemented()

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

    override suspend fun createTransaction(body: com.dhruv.finance.data.tracker.dto.TransactionUpsertDto) = unimplemented()

    override suspend fun updateTransaction(
        id: String,
        body: com.dhruv.finance.data.tracker.dto.TransactionUpsertDto,
    ) = unimplemented()

    override suspend fun patchTransaction(
        id: String,
        body: Map<String, Any?>,
    ) = unimplemented()

    override suspend fun listRecurringTemplates() = unimplemented()

    override suspend fun createRecurringTemplate(body: com.dhruv.finance.data.tracker.dto.RecurringTemplateUpsertDto) = unimplemented()

    override suspend fun setRecurringPaused(
        id: String,
        body: com.dhruv.finance.data.tracker.dto.RecurringPauseDto,
    ) = unimplemented()

    override suspend fun advanceRecurringNextRun(
        id: String,
        body: Map<String, String>,
    ) = unimplemented()

    override suspend fun editRecurringTemplate(
        id: String,
        body: com.dhruv.finance.data.tracker.dto.RecurringTemplateEditDto,
    ) = unimplemented()

    override suspend fun softDeleteRecurringTemplate(
        id: String,
        body: Map<String, String>,
    ) = unimplemented()

    override suspend fun dismissPendingForRecurring(
        recurringIdFilter: String,
        body: com.dhruv.finance.data.tracker.dto.SuggestionStatusDto,
        statusFilter: String,
    ) = unimplemented()

    override suspend fun listPendingSuggestions() = unimplemented()

    override suspend fun createSuggestion(body: com.dhruv.finance.data.tracker.dto.SuggestionUpsertDto) = unimplemented()

    override suspend fun setSuggestionStatus(
        id: String,
        body: com.dhruv.finance.data.tracker.dto.SuggestionStatusDto,
    ) = unimplemented()
}

class TransactionAuditTest {
    // One row per kind the trigger can produce (fn_transaction_audit.sql's INSERT branch dispatch
    // on NEW.source, and the UPDATE branch's soft-delete/category-change/generic-edit checks) —
    // proves the mapper round-trips every kind value the DB actually writes, not just a subset.
    @Test
    fun `listEvents maps every transaction_events kind the trigger can produce`() =
        runTest {
            val dtos =
                listOf(
                    TransactionEventDto(
                        id = "evt-1",
                        transactionId = "txn-1",
                        at = "2026-09-01T10:00:00Z",
                        kind = "CREATED",
                        detail = mapOf("type" to "EXPENSE", "amount_paise" to 50_00),
                    ),
                    TransactionEventDto(
                        id = "evt-2",
                        transactionId = "txn-1",
                        at = "2026-09-02T10:00:00Z",
                        kind = "EDITED",
                        detail = mapOf("payee" to mapOf("old" to "Coffee shop", "new" to "Cafe")),
                    ),
                    TransactionEventDto(
                        id = "evt-3",
                        transactionId = "txn-1",
                        at = "2026-09-03T10:00:00Z",
                        kind = "CATEGORY_CHANGED",
                        detail = mapOf("old_category_id" to "cat-1", "new_category_id" to "cat-2"),
                    ),
                    TransactionEventDto(
                        id = "evt-4",
                        transactionId = "txn-1",
                        at = "2026-09-04T10:00:00Z",
                        kind = "RECONCILED",
                        detail = mapOf("source" to "RECONCILE"),
                    ),
                    TransactionEventDto(
                        id = "evt-5",
                        transactionId = "txn-1",
                        at = "2026-09-05T10:00:00Z",
                        kind = "ACCEPTED_FROM_RECURRING",
                        detail = mapOf("source" to "RECURRING"),
                    ),
                    TransactionEventDto(
                        id = "evt-6",
                        transactionId = "txn-1",
                        at = "2026-09-06T10:00:00Z",
                        kind = "DELETED",
                        detail = mapOf("deleted_at" to "2026-09-06T10:00:00Z"),
                    ),
                )
            val api =
                object : FakeAuditMoneyApi() {
                    override suspend fun listTransactionEvents(
                        transactionId: String,
                        order: String,
                    ) = dtos
                }
            val repo: TransactionRepository = TransactionRepositoryImpl(api)

            val events = repo.listEvents("txn-1").getOrThrow()

            assertEquals(6, events.size)
            assertEquals(
                listOf(
                    TransactionEventKind.CREATED,
                    TransactionEventKind.EDITED,
                    TransactionEventKind.CATEGORY_CHANGED,
                    TransactionEventKind.RECONCILED,
                    TransactionEventKind.ACCEPTED_FROM_RECURRING,
                    TransactionEventKind.DELETED,
                ),
                events.map { it.kind },
            )
        }

    // The repository must not re-sort — ordering is the server's job (`order=at.asc` is the API's
    // own default, MoneyApi.kt), and a client-side resort could silently mask a server regression.
    // This proves the mapping layer passes the server's order straight through unchanged.
    @Test
    fun `listEvents preserves the server's ordering and does not re-sort client-side`() =
        runTest {
            val dtos =
                listOf(
                    TransactionEventDto(id = "evt-2", transactionId = "txn-1", at = "2026-09-02T10:00:00Z", kind = "EDITED"),
                    TransactionEventDto(id = "evt-1", transactionId = "txn-1", at = "2026-09-01T10:00:00Z", kind = "CREATED"),
                )
            val api =
                object : FakeAuditMoneyApi() {
                    override suspend fun listTransactionEvents(
                        transactionId: String,
                        order: String,
                    ) = dtos
                }
            val repo: TransactionRepository = TransactionRepositoryImpl(api)

            val events = repo.listEvents("txn-1").getOrThrow()

            assertEquals(listOf("evt-2", "evt-1"), events.map { it.id })
        }

    // CATEGORY_CHANGED/EDITED detail payloads (old/new values) must survive the round trip
    // unchanged — the ViewModel renders plain-language history straight from these keys (T051).
    @Test
    fun `listEvents preserves detail payload keys and values for rendering`() =
        runTest {
            val dto =
                TransactionEventDto(
                    id = "evt-1",
                    transactionId = "txn-1",
                    at = "2026-09-01T10:00:00Z",
                    kind = "CATEGORY_CHANGED",
                    detail = mapOf("old_category_id" to "cat-1", "new_category_id" to "cat-2"),
                )
            val api =
                object : FakeAuditMoneyApi() {
                    override suspend fun listTransactionEvents(
                        transactionId: String,
                        order: String,
                    ) = listOf(dto)
                }
            val repo: TransactionRepository = TransactionRepositoryImpl(api)

            val event = repo.listEvents("txn-1").getOrThrow().single()

            assertTrue(event.detail != null)
            assertEquals("cat-1", event.detail?.get("old_category_id"))
            assertEquals("cat-2", event.detail?.get("new_category_id"))
        }
}
