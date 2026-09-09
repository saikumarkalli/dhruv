package com.dhruv.finance.data.tracker.net

import com.dhruv.finance.data.tracker.dto.AccountBalanceDto
import com.dhruv.finance.data.tracker.dto.AccountDto
import com.dhruv.finance.data.tracker.dto.AccountUpsertDto
import com.dhruv.finance.data.tracker.dto.CategoryDto
import com.dhruv.finance.data.tracker.dto.CategoryRenameDto
import com.dhruv.finance.data.tracker.dto.CategorySpendDto
import com.dhruv.finance.data.tracker.dto.CategoryUpsertDto
import com.dhruv.finance.data.tracker.dto.MergeCategoriesRequestDto
import com.dhruv.finance.data.tracker.dto.MonthSummaryDto
import com.dhruv.finance.data.tracker.dto.RecurringPauseDto
import com.dhruv.finance.data.tracker.dto.RecurringTemplateDto
import com.dhruv.finance.data.tracker.dto.RecurringTemplateEditDto
import com.dhruv.finance.data.tracker.dto.RecurringTemplateUpsertDto
import com.dhruv.finance.data.tracker.dto.SuggestionDto
import com.dhruv.finance.data.tracker.dto.SuggestionStatusDto
import com.dhruv.finance.data.tracker.dto.SuggestionUpsertDto
import com.dhruv.finance.data.tracker.dto.TransactionCountRowDto
import com.dhruv.finance.data.tracker.dto.TransactionDto
import com.dhruv.finance.data.tracker.dto.TransactionEventDto
import com.dhruv.finance.data.tracker.dto.TransactionUpsertDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * PostgREST table/view/RPC endpoints for the Money tab (002-money-tab). Built off
 * [SupabaseClientFactory.dataRetrofit] — consent-gated, auth-gated, and `finance`-schema-profiled
 * ([FinanceSchemaInterceptor]) by construction, so no call site here can reach PostgREST without
 * going through both gates. RLS (`user_id = auth.uid()`, directly or transitively) means no query
 * here filters by user — the server already scopes every row to the caller.
 *
 * Filter params take a full PostgREST operator string (e.g. `"eq.$id"`, `"gte.2026-09-01"`) —
 * callers build that string, this interface only names which column it applies to.
 * `Prefer: return=representation` on every write so PostgREST echoes back the row(s) it just
 * wrote — callers `.first()` the array rather than issue a second read.
 */
interface MoneyApi {
    // ── Accounts ────────────────────────────────────────────────────────────────────────────
    @GET("accounts?deleted_at=is.null&order=is_primary.desc,created_at.asc")
    suspend fun listAccounts(): List<AccountDto>

    @GET("v_account_balances")
    suspend fun listAccountBalances(): List<AccountBalanceDto>

    @Headers("Prefer: return=representation")
    @POST("accounts")
    suspend fun createAccount(
        @Body body: AccountUpsertDto,
    ): List<AccountDto>

    @Headers("Prefer: return=representation")
    @PATCH("accounts")
    suspend fun updateAccount(
        @Query("id") id: String,
        @Body body: AccountUpsertDto,
    ): List<AccountDto>

    @Headers("Prefer: return=representation")
    @PATCH("accounts")
    suspend fun patchAccount(
        @Query("id") id: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): List<AccountDto>

    /** Exact, all-time transaction count for one account (FR-021a, Edge Cases — "the user must be
     * told what happens to those transactions before the deletion is confirmed"). Same
     * `Prefer: count=exact` pattern as [countTransactionsForCategory]; counts the account as the
     * primary `account_id` only, not a transfer's `to_account_id` side. */
    @Headers("Prefer: count=exact")
    @GET("transactions?deleted_at=is.null&limit=1&select=id")
    suspend fun countTransactionsForAccount(
        @Query("account_id") accountId: String,
    ): Response<List<TransactionCountRowDto>>

    // ── Categories ──────────────────────────────────────────────────────────────────────────
    @GET("categories?deleted_at=is.null&order=name.asc")
    suspend fun listCategories(): List<CategoryDto>

    @GET("v_category_spend")
    suspend fun listCategorySpend(
        @Query("month") month: String,
    ): List<CategorySpendDto>

    @Headers("Prefer: return=representation")
    @POST("categories")
    suspend fun createCategory(
        @Body body: CategoryUpsertDto,
    ): List<CategoryDto>

    @Headers("Prefer: return=representation")
    @PATCH("categories")
    suspend fun renameCategory(
        @Query("id") id: String,
        @Body body: CategoryRenameDto,
    ): List<CategoryDto>

    @Headers("Prefer: return=representation")
    @PATCH("categories")
    suspend fun updateCategoryExcluded(
        @Query("id") id: String,
        @Body body: Map<String, Boolean>,
    ): List<CategoryDto>

    @Headers("Prefer: return=representation")
    @PATCH("categories")
    suspend fun softDeleteCategory(
        @Query("id") id: String,
        @Body body: Map<String, String>,
    ): List<CategoryDto>

    @POST("rpc/merge_categories")
    suspend fun mergeCategories(
        @Body body: MergeCategoriesRequestDto,
    ): Int

    /** Exact, all-time transaction count for one category, via PostgREST's `Prefer: count=exact`
     * — the merge confirmation (FR-024) needs the true count of everything `merge_categories`
     * will move, not a client-side re-derivation from a month-scoped listing. `limit=1&select=id`
     * keeps the body itself minimal; the count lives in the response's `Content-Range` header
     * (`0-0/<total>`), read by [com.dhruv.finance.data.tracker.repo.CategoryRepositoryImpl]. */
    @Headers("Prefer: count=exact")
    @GET("transactions?deleted_at=is.null&limit=1&select=id")
    suspend fun countTransactionsForCategory(
        @Query("category_id") categoryId: String,
    ): Response<List<TransactionCountRowDto>>

    // ── Transactions ────────────────────────────────────────────────────────────────────────
    @GET("transactions?deleted_at=is.null&order=occurred_at.desc")
    suspend fun listTransactions(
        @Query("occurred_at") occurredAtGte: String,
        @Query("occurred_at") occurredAtLt: String,
    ): List<TransactionDto>

    @GET("transactions")
    suspend fun getTransaction(
        @Query("id") id: String,
    ): List<TransactionDto>

    @GET("transaction_events")
    suspend fun listTransactionEvents(
        @Query("transaction_id") transactionId: String,
        @Query("order") order: String = "at.asc",
    ): List<TransactionEventDto>

    @GET("v_month_summary")
    suspend fun getMonthSummary(
        @Query("month") month: String,
    ): List<MonthSummaryDto>

    @Headers("Prefer: return=representation")
    @POST("transactions")
    suspend fun createTransaction(
        @Body body: TransactionUpsertDto,
    ): List<TransactionDto>

    @Headers("Prefer: return=representation")
    @PATCH("transactions")
    suspend fun updateTransaction(
        @Query("id") id: String,
        @Body body: TransactionUpsertDto,
    ): List<TransactionDto>

    @Headers("Prefer: return=representation")
    @PATCH("transactions")
    suspend fun patchTransaction(
        @Query("id") id: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): List<TransactionDto>

    // ── Recurring + Suggestions ─────────────────────────────────────────────────────────────
    @GET("recurring_templates?deleted_at=is.null&order=next_run.asc")
    suspend fun listRecurringTemplates(): List<RecurringTemplateDto>

    @Headers("Prefer: return=representation")
    @POST("recurring_templates")
    suspend fun createRecurringTemplate(
        @Body body: RecurringTemplateUpsertDto,
    ): List<RecurringTemplateDto>

    @Headers("Prefer: return=representation")
    @PATCH("recurring_templates")
    suspend fun setRecurringPaused(
        @Query("id") id: String,
        @Body body: RecurringPauseDto,
    ): List<RecurringTemplateDto>

    @Headers("Prefer: return=representation")
    @PATCH("recurring_templates")
    suspend fun advanceRecurringNextRun(
        @Query("id") id: String,
        @Body body: Map<String, String>,
    ): List<RecurringTemplateDto>

    @Headers("Prefer: return=representation")
    @PATCH("recurring_templates")
    suspend fun editRecurringTemplate(
        @Query("id") id: String,
        @Body body: RecurringTemplateEditDto,
    ): List<RecurringTemplateDto>

    @PATCH("recurring_templates")
    suspend fun softDeleteRecurringTemplate(
        @Query("id") id: String,
        @Body body: Map<String, String>,
    )

    /** FR-031b — withdraws every still-pending suggestion a deleted recurring definition produced,
     * so none is left actionable in the review queue. */
    @PATCH("suggestions")
    suspend fun dismissPendingForRecurring(
        @Query("recurring_id") recurringIdFilter: String,
        @Body body: SuggestionStatusDto,
        @Query("status") statusFilter: String = "eq.PENDING",
    )

    @GET("suggestions?status=eq.PENDING&order=due_on.asc")
    suspend fun listPendingSuggestions(): List<SuggestionDto>

    @Headers("Prefer: return=representation,resolution=ignore-duplicates")
    @POST("suggestions")
    suspend fun createSuggestion(
        @Body body: SuggestionUpsertDto,
    ): List<SuggestionDto>

    @Headers("Prefer: return=representation")
    @PATCH("suggestions")
    suspend fun setSuggestionStatus(
        @Query("id") id: String,
        @Body body: SuggestionStatusDto,
    ): List<SuggestionDto>
}
