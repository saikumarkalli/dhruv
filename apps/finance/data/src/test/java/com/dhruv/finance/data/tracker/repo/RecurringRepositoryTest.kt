package com.dhruv.finance.data.tracker.repo

import com.dhruv.finance.data.tracker.dto.AccountUpsertDto
import com.dhruv.finance.data.tracker.dto.CategoryRenameDto
import com.dhruv.finance.data.tracker.dto.CategoryUpsertDto
import com.dhruv.finance.data.tracker.dto.MergeCategoriesRequestDto
import com.dhruv.finance.data.tracker.dto.RecurringPauseDto
import com.dhruv.finance.data.tracker.dto.RecurringTemplateDto
import com.dhruv.finance.data.tracker.dto.RecurringTemplateUpsertDto
import com.dhruv.finance.data.tracker.dto.SuggestionDto
import com.dhruv.finance.data.tracker.dto.SuggestionStatusDto
import com.dhruv.finance.data.tracker.dto.SuggestionUpsertDto
import com.dhruv.finance.data.tracker.dto.TransactionUpsertDto
import com.dhruv.finance.data.tracker.net.MoneyApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private open class RecurringFakeMoneyApi : MoneyApi by RecurringUnimplementedMoneyApi

/** `internal`, not `private` — [SuggestionRepositoryTest] in the same module shares this fake
 * rather than duplicating the same ~30-method boilerplate a second time. */
internal object RecurringUnimplementedMoneyApi : MoneyApi {
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
        body: SuggestionStatusDto,
        statusFilter: String,
    ) = unimplemented()

    override suspend fun listPendingSuggestions() = unimplemented()

    override suspend fun createSuggestion(body: SuggestionUpsertDto) = unimplemented()

    override suspend fun setSuggestionStatus(
        id: String,
        body: SuggestionStatusDto,
    ) = unimplemented()
}

private fun templateDto(
    id: String = "rec-1",
    nextRun: String = "2026-09-01",
    paused: Boolean = false,
) = RecurringTemplateDto(
    id = id,
    template = mapOf("type" to "EXPENSE", "amountPaise" to 5_00L, "accountId" to "acc-1", "categoryId" to "cat-1"),
    rrule = "FREQ=MONTHLY",
    nextRun = nextRun,
    paused = paused,
)

/** A fake [SuggestionRepository] that simulates the DB's unique `(recurring_id, due_on)`
 * constraint — a second `createFromRecurring` for the same template/date is silently absorbed
 * (`Prefer: resolution=ignore-duplicates`), exactly like a real duplicate insert. */
private class FakeSuggestionRepository : SuggestionRepository {
    val createdKeys = mutableSetOf<Pair<String, String>>()

    override suspend fun listPending() = Result.success(emptyList<com.dhruv.finance.data.tracker.model.PendingEntry>())

    override suspend fun createFromRecurring(
        template: com.dhruv.finance.data.tracker.model.RecurringTemplate,
    ): Result<Unit> {
        createdKeys += template.id to template.nextRun.toString()
        return Result.success(Unit)
    }

    override suspend fun accept(entry: com.dhruv.finance.data.tracker.model.PendingEntry) = error("not stubbed for this test")

    override suspend fun dismiss(entryId: String) = error("not stubbed for this test")
}

class RecurringRepositoryTest {
    // MNY-BR-005: a due template's occurrence creates a `suggestions` row, never a `transactions`
    // row — proven here by FakeSuggestionRepository.createFromRecurring being called and nothing
    // else (no transaction-creating call exists in this fake's surface at all).
    @Test
    fun `a due template materialises into exactly one pending entry, never a transaction`() =
        runTest {
            var advanceCalled = false
            val api =
                object : RecurringFakeMoneyApi() {
                    override suspend fun listRecurringTemplates() = listOf(templateDto(nextRun = "2026-09-01"))

                    override suspend fun advanceRecurringNextRun(
                        id: String,
                        body: Map<String, String>,
                    ): List<RecurringTemplateDto> {
                        advanceCalled = true
                        return listOf(templateDto(nextRun = body.getValue("next_run")))
                    }
                }
            val repo: RecurringRepository = RecurringRepositoryImpl(api)
            val suggestions = FakeSuggestionRepository()

            val result = repo.materialiseDue(suggestions, today = LocalDate.of(2026, 9, 5))

            assertTrue(result.isSuccess)
            assertEquals(1, suggestions.createdKeys.size)
            assertTrue(advanceCalled)
        }

    @Test
    fun `a template whose next_run is in the future is not materialised`() =
        runTest {
            val api =
                object : RecurringFakeMoneyApi() {
                    override suspend fun listRecurringTemplates() = listOf(templateDto(nextRun = "2026-10-01"))
                }
            val repo: RecurringRepository = RecurringRepositoryImpl(api)
            val suggestions = FakeSuggestionRepository()

            repo.materialiseDue(suggestions, today = LocalDate.of(2026, 9, 5))

            assertTrue(suggestions.createdKeys.isEmpty())
        }

    @Test
    fun `a paused template is never materialised`() =
        runTest {
            val api =
                object : RecurringFakeMoneyApi() {
                    override suspend fun listRecurringTemplates() = listOf(templateDto(nextRun = "2026-09-01", paused = true))
                }
            val repo: RecurringRepository = RecurringRepositoryImpl(api)
            val suggestions = FakeSuggestionRepository()

            repo.materialiseDue(suggestions, today = LocalDate.of(2026, 9, 5))

            assertTrue(suggestions.createdKeys.isEmpty())
        }

    // MNY-BR-005 (idempotency half): materialising twice (two app opens, or two devices)
    // produces exactly one pending entry — the (recurring_id, due_on) key stays constant across
    // repeated calls for the same still-due occurrence, so a real unique-constraint insert would
    // collide harmlessly on the second call, same as this fake's set-based dedup.
    @Test
    fun `materialising the same due occurrence twice produces exactly one key`() =
        runTest {
            val api =
                object : RecurringFakeMoneyApi() {
                    override suspend fun listRecurringTemplates() = listOf(templateDto(nextRun = "2026-09-01"))

                    override suspend fun advanceRecurringNextRun(
                        id: String,
                        body: Map<String, String>,
                    ) = listOf(templateDto(nextRun = "2026-09-01")) // simulate: not yet advanced server-side
                }
            val repo: RecurringRepository = RecurringRepositoryImpl(api)
            val suggestions = FakeSuggestionRepository()

            repo.materialiseDue(suggestions, today = LocalDate.of(2026, 9, 5))
            repo.materialiseDue(suggestions, today = LocalDate.of(2026, 9, 5))

            assertEquals(1, suggestions.createdKeys.size)
        }

    @Test
    fun `createFromTransaction writes only the recurring_templates row`() =
        runTest {
            var createCalled = false
            val api =
                object : RecurringFakeMoneyApi() {
                    override suspend fun createRecurringTemplate(body: RecurringTemplateUpsertDto): List<RecurringTemplateDto> {
                        createCalled = true
                        return listOf(templateDto())
                    }
                }
            val repo: RecurringRepository = RecurringRepositoryImpl(api)
            val transaction =
                com.dhruv.finance.data.tracker.model.Transaction(
                    id = "txn-1",
                    type = com.dhruv.finance.data.tracker.model.TransactionType.EXPENSE,
                    amountPaise = 5_00,
                    accountId = "acc-1",
                    toAccountId = null,
                    categoryId = "cat-1",
                    payee = "Landlord",
                    note = null,
                    occurredAt = java.time.Instant.parse("2026-09-01T10:00:00Z"),
                    cleared = true,
                    receiptPath = null,
                    goalId = null,
                    recurringId = null,
                    splitGroupId = null,
                    source = com.dhruv.finance.data.tracker.model.TransactionSource.MANUAL,
                )

            val result = repo.createFromTransaction(transaction, rrule = "FREQ=MONTHLY", nextRun = LocalDate.of(2026, 10, 1))

            assertTrue(result.isSuccess)
            assertTrue(createCalled)
        }

    // FR-031a: edit writes only the recurring_templates row, never a transaction or suggestion.
    @Test
    fun `edit sends the new amount, category, account and schedule`() =
        runTest {
            var sentId: String? = null
            var sentBody: com.dhruv.finance.data.tracker.dto.RecurringTemplateEditDto? = null
            val api =
                object : RecurringFakeMoneyApi() {
                    override suspend fun editRecurringTemplate(
                        id: String,
                        body: com.dhruv.finance.data.tracker.dto.RecurringTemplateEditDto,
                    ): List<RecurringTemplateDto> {
                        sentId = id
                        sentBody = body
                        return listOf(templateDto(id = "rec-1", nextRun = body.nextRun))
                    }
                }
            val repo: RecurringRepository = RecurringRepositoryImpl(api)

            val result =
                repo.edit(
                    templateId = "rec-1",
                    type = com.dhruv.finance.data.tracker.model.TransactionType.EXPENSE,
                    amountPaise = 7_50,
                    accountId = "acc-2",
                    categoryId = "cat-2",
                    payee = "Landlord",
                    note = "rent",
                    rrule = "FREQ=WEEKLY",
                    nextRun = LocalDate.of(2026, 10, 1),
                    amountIsVariable = true,
                )

            assertTrue(result.isSuccess)
            assertEquals("eq.rec-1", sentId)
            assertEquals("FREQ=WEEKLY", sentBody?.rrule)
            assertEquals("2026-10-01", sentBody?.nextRun)
            assertEquals(7_50L, sentBody?.template?.get("amountPaise"))
            assertEquals("acc-2", sentBody?.template?.get("accountId"))
        }

    // FR-031b: delete soft-deletes the template AND withdraws every still-pending suggestion it
    // produced — the two calls a real deletion must make, neither of which is a hard DELETE.
    @Test
    fun `delete soft-deletes the template and dismisses its pending suggestions`() =
        runTest {
            var deletedId: String? = null
            var dismissedFilter: String? = null
            var dismissedStatus: String? = null
            val api =
                object : RecurringFakeMoneyApi() {
                    override suspend fun softDeleteRecurringTemplate(
                        id: String,
                        body: Map<String, String>,
                    ) {
                        deletedId = id
                    }

                    override suspend fun dismissPendingForRecurring(
                        recurringIdFilter: String,
                        body: SuggestionStatusDto,
                        statusFilter: String,
                    ) {
                        dismissedFilter = recurringIdFilter
                        dismissedStatus = body.status
                    }
                }
            val repo: RecurringRepository = RecurringRepositoryImpl(api)

            val result = repo.delete("rec-1")

            assertTrue(result.isSuccess)
            assertEquals("eq.rec-1", deletedId)
            assertEquals("eq.rec-1", dismissedFilter)
            assertEquals("IGNORED", dismissedStatus)
        }

    // Edge Cases: "a pending recurring entry for a paused ... recurring definition must not remain
    // actionable" — pause withdraws the same way delete does.
    @Test
    fun `pause dismisses the template's pending suggestions too`() =
        runTest {
            var pausedId: String? = null
            var dismissedFilter: String? = null
            val api =
                object : RecurringFakeMoneyApi() {
                    override suspend fun setRecurringPaused(
                        id: String,
                        body: RecurringPauseDto,
                    ): List<RecurringTemplateDto> {
                        pausedId = id
                        return listOf(templateDto(id = "rec-1", paused = true))
                    }

                    override suspend fun dismissPendingForRecurring(
                        recurringIdFilter: String,
                        body: SuggestionStatusDto,
                        statusFilter: String,
                    ) {
                        dismissedFilter = recurringIdFilter
                    }
                }
            val repo: RecurringRepository = RecurringRepositoryImpl(api)

            val result = repo.pause("rec-1")

            assertTrue(result.isSuccess)
            assertEquals("eq.rec-1", pausedId)
            assertEquals("eq.rec-1", dismissedFilter)
        }
}
