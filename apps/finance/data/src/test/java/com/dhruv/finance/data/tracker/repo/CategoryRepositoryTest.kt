package com.dhruv.finance.data.tracker.repo

import com.dhruv.finance.data.tracker.dto.CategoryDto
import com.dhruv.finance.data.tracker.dto.CategoryRenameDto
import com.dhruv.finance.data.tracker.dto.CategorySpendDto
import com.dhruv.finance.data.tracker.dto.CategoryUpsertDto
import com.dhruv.finance.data.tracker.dto.MergeCategoriesRequestDto
import com.dhruv.finance.data.tracker.dto.TransactionCountRowDto
import com.dhruv.finance.data.tracker.net.MoneyApi
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.time.YearMonth

/**
 * A partial [MoneyApi] fake — every method not exercised by a given test throws, so a test that
 * hits an un-stubbed call fails loudly instead of returning a silent default (mirrors
 * [com.dhruv.finance.data.tracker.repo.TransactionRepositoryTest]'s `FakeMoneyApi` convention).
 */
private open class CategoryFakeMoneyApi : MoneyApi by CategoryUnimplementedMoneyApi

private object CategoryUnimplementedMoneyApi : MoneyApi {
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

private fun category(
    id: String = "cat-1",
    name: String = "Groceries",
    kind: String = "EXPENSE",
    excludedFromSpend: Boolean = false,
) = CategoryDto(id = id, name = name, kind = kind, excludedFromSpend = excludedFromSpend)

class CategoryRepositoryTest {
    // T055 / MNY-BR-003: a rename PATCHes only `name` — id and every linked transaction are
    // untouched because nothing else about the row (or any transaction's category_id FK) changes.
    @Test
    fun `rename sends only the new name and preserves the category's id`() =
        runTest {
            var capturedId: String? = null
            var capturedBody: CategoryRenameDto? = null
            val api =
                object : CategoryFakeMoneyApi() {
                    override suspend fun renameCategory(
                        id: String,
                        body: CategoryRenameDto,
                    ): List<CategoryDto> {
                        capturedId = id
                        capturedBody = body
                        return listOf(category(id = "cat-1", name = body.name))
                    }
                }
            val repo: CategoryRepository = CategoryRepositoryImpl(api)

            val result = repo.renameCategory("cat-1", "Groceries & Household")

            assertTrue(result.isSuccess)
            assertEquals("cat-1", result.getOrThrow().id)
            assertEquals("Groceries & Household", result.getOrThrow().name)
            assertEquals("eq.cat-1", capturedId)
            assertEquals(CategoryRenameDto("Groceries & Household"), capturedBody)
        }

    // T056 / MNY-BR-004: merge is the RPC call and nothing else — the repository passes source
    // and target through unchanged and returns the server's moved-count unchanged (the same
    // count the confirmation dialog must have already stated, per FR-024). "the source category
    // ends soft-deleted" is `merge_categories`' own server-side behaviour (data-model.md) — not
    // observable from a JVM test with no database, so this proves the Kotlin boundary: the exact
    // ids requested and the exact count returned, both unmodified in transit.
    @Test
    fun `merge passes source and target through unchanged and returns the server's moved count`() =
        runTest {
            var captured: MergeCategoriesRequestDto? = null
            val api =
                object : CategoryFakeMoneyApi() {
                    override suspend fun mergeCategories(body: MergeCategoriesRequestDto): Int {
                        captured = body
                        return 19 // N (source) + M (target), per MNY-BR-004's dialog wording
                    }
                }
            val repo: CategoryRepository = CategoryRepositoryImpl(api)

            val result = repo.mergeCategories(sourceId = "cat-source", targetId = "cat-target")

            assertTrue(result.isSuccess)
            assertEquals(19, result.getOrThrow())
            assertEquals(MergeCategoriesRequestDto(source = "cat-source", target = "cat-target"), captured)
        }

    // T058 / FR-025: v_category_spend already excludes an `excluded_from_spend` category from the
    // OTHER rows' totals server-side (research: same exclusion as v_month_summary). What the
    // Kotlin mapping layer must not do is re-zero or re-derive that category's own spend/share —
    // it passes the server's numbers straight through, exactly like the existing
    // TransactionRepositoryTest convention for v_category_spend/v_month_summary mapping.
    @Test
    fun `listCategoriesWithSpend passes an excluded category's server-computed spend and share through unchanged`() =
        runTest {
            val excluded = category(id = "cat-invest", name = "Investment", excludedFromSpend = true)
            val api =
                object : CategoryFakeMoneyApi() {
                    override suspend fun listCategories() = listOf(excluded)

                    override suspend fun listCategorySpend(month: String) =
                        listOf(
                            CategorySpendDto(
                                month = month,
                                categoryId = "cat-invest",
                                categoryName = "Investment",
                                categoryKind = "EXPENSE",
                                excludedFromSpend = true,
                                spendPaise = 500_00,
                                sharePercent = 0.0,
                            ),
                        )
                }
            val repo: CategoryRepository = CategoryRepositoryImpl(api)

            val result = repo.listCategoriesWithSpend(YearMonth.of(2026, 9)).getOrThrow()

            val row = result.single()
            assertTrue(row.excludedFromSpend)
            assertEquals(500_00L, row.spendPaise)
            assertEquals(0.0, row.sharePercent)
        }

    // Companion case: a category absent from this month's v_category_spend rows (no spend at
    // all, not even zero) comes back with null spend/share rather than an invented 0 — a category
    // with genuinely no data this month is not the same fact as "spent zero".
    @Test
    fun `listCategoriesWithSpend leaves spend and share null for a category absent from this month's spend view`() =
        runTest {
            val api =
                object : CategoryFakeMoneyApi() {
                    override suspend fun listCategories() = listOf(category(id = "cat-unused", name = "Unused"))

                    override suspend fun listCategorySpend(month: String) = emptyList<CategorySpendDto>()
                }
            val repo: CategoryRepository = CategoryRepositoryImpl(api)

            val row = repo.listCategoriesWithSpend(YearMonth.of(2026, 9)).getOrThrow().single()

            assertNull(row.spendPaise)
            assertNull(row.sharePercent)
        }

    // countTransactionsForCategory: reads the exact total from PostgREST's `Content-Range`
    // response header (`Prefer: count=exact`) rather than the (deliberately minimal) response
    // body — this is the all-time count no server view otherwise provides (FR-024/FR-026).
    @Test
    fun `countTransactionsForCategory reads the exact total from the Content-Range header`() =
        runTest {
            val api =
                object : CategoryFakeMoneyApi() {
                    override suspend fun countTransactionsForCategory(categoryId: String) =
                        Response.success(
                            listOf(TransactionCountRowDto(id = "txn-1")),
                            Headers.headersOf("Content-Range", "0-0/42"),
                        )
                }
            val repo: CategoryRepository = CategoryRepositoryImpl(api)

            val result = repo.countTransactionsForCategory("cat-1")

            assertTrue(result.isSuccess)
            assertEquals(42, result.getOrThrow())
        }

    @Test
    fun `countTransactionsForCategory is zero when Content-Range reports an empty range`() =
        runTest {
            val api =
                object : CategoryFakeMoneyApi() {
                    override suspend fun countTransactionsForCategory(categoryId: String) =
                        Response.success(emptyList<TransactionCountRowDto>(), Headers.headersOf("Content-Range", "*/0"))
                }
            val repo: CategoryRepository = CategoryRepositoryImpl(api)

            val result = repo.countTransactionsForCategory("cat-empty")

            assertTrue(result.isSuccess)
            assertEquals(0, result.getOrThrow())
        }

    @Test
    fun `countTransactionsForCategory fails rather than inventing a count when Content-Range is missing`() =
        runTest {
            val api =
                object : CategoryFakeMoneyApi() {
                    override suspend fun countTransactionsForCategory(categoryId: String) =
                        Response.success(emptyList<TransactionCountRowDto>())
                }
            val repo: CategoryRepository = CategoryRepositoryImpl(api)

            val result = repo.countTransactionsForCategory("cat-1")

            assertFalse(result.isSuccess)
        }
}
