package com.dhruv.finance.data.tracker.repo

import com.dhruv.finance.data.tracker.dto.MergeCategoriesRequestDto
import com.dhruv.finance.data.tracker.mapper.toDomain
import com.dhruv.finance.data.tracker.mapper.toUpsertDto
import com.dhruv.finance.data.tracker.model.Category
import com.dhruv.finance.data.tracker.net.MoneyApi
import com.dhruv.finance.data.tracker.net.SupabaseClientFactory
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.time.YearMonth
import java.util.UUID

/** Money tab categories (D8, US5). [mergeCategories] is the ONLY merge path (FR-024, R9) — it
 * calls the `merge_categories` RPC, never a client-side loop. */
interface CategoryRepository {
    suspend fun listCategories(): Result<List<Category>>

    /** Same rows as [listCategories], joined against `finance.v_category_spend` for [month] —
     * each [Category.spendPaise]/[Category.sharePercent] is server-computed (NFR-8), never
     * re-derived client-side. A category with no spend this month (or genuinely excluded from
     * the view) comes back with both fields `null`, not zero. */
    suspend fun listCategoriesWithSpend(month: YearMonth): Result<List<Category>>

    suspend fun createCategory(category: Category): Result<Category>

    suspend fun renameCategory(
        categoryId: String,
        newName: String,
    ): Result<Category>

    suspend fun setExcludedFromSpend(
        categoryId: String,
        excluded: Boolean,
    ): Result<Category>

    /** Returns the number of transactions moved — the same number the confirmation dialog stated
     * (FR-024, MNY-BR-004). */
    suspend fun mergeCategories(
        sourceId: String,
        targetId: String,
    ): Result<Int>

    suspend fun softDeleteCategory(categoryId: String): Result<Unit>

    /** Ensures the two reserved rows (Uncategorised, Adjustment) exist for this user, creating
     * whichever is missing. Idempotent — safe to call on every categories-screen open. */
    suspend fun ensureReservedCategories(): Result<Unit>

    /** Exact, all-time count of non-deleted transactions linked to [categoryId] — what the D8
     * merge confirmation (FR-024) and the Uncategorised row's "N need a category" (FR-026) both
     * need, and what no server view currently provides (`v_category_spend` is month-scoped
     * money, not an all-time count). Backed by PostgREST's `Prefer: count=exact`, so this is a
     * real server-side count, not a client re-derivation from a partial listing. */
    suspend fun countTransactionsForCategory(categoryId: String): Result<Int>
}

class CategoryRepositoryImpl(
    private val api: MoneyApi,
) : CategoryRepository {
    constructor(supabaseClientFactory: SupabaseClientFactory) : this(
        supabaseClientFactory.dataRetrofit.create(MoneyApi::class.java),
    )

    @Suppress("TooGenericExceptionCaught")
    override suspend fun listCategories(): Result<List<Category>> =
        try {
            Result.success(api.listCategories().map { it.toDomain() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun listCategoriesWithSpend(month: YearMonth): Result<List<Category>> =
        try {
            val monthStart = month.atDay(1).toString()
            val spendByCategory = api.listCategorySpend("eq.$monthStart").associateBy { it.categoryId }
            val categories = api.listCategories()
            Result.success(categories.map { it.toDomain(spendByCategory[it.id]) })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun createCategory(category: Category): Result<Category> =
        try {
            val requestId = UUID.randomUUID().toString()
            val created = api.createCategory(category.toUpsertDto(requestId)).first()
            Result.success(created.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun renameCategory(
        categoryId: String,
        newName: String,
    ): Result<Category> =
        try {
            val renamed =
                api
                    .renameCategory(
                        "eq.$categoryId",
                        com.dhruv.finance.data.tracker.dto.CategoryRenameDto(newName),
                    ).first()
            Result.success(renamed.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun setExcludedFromSpend(
        categoryId: String,
        excluded: Boolean,
    ): Result<Category> =
        try {
            val updated = api.updateCategoryExcluded("eq.$categoryId", mapOf("excluded_from_spend" to excluded)).first()
            Result.success(updated.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun mergeCategories(
        sourceId: String,
        targetId: String,
    ): Result<Int> =
        try {
            Result.success(api.mergeCategories(MergeCategoriesRequestDto(sourceId, targetId)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun softDeleteCategory(categoryId: String): Result<Unit> =
        try {
            api.softDeleteCategory("eq.$categoryId", mapOf("deleted_at" to Instant.now().toString()))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun ensureReservedCategories(): Result<Unit> =
        try {
            val existing = api.listCategories().map { it.name }
            if (Category.RESERVED_UNCATEGORISED !in existing) {
                api.createCategory(
                    com.dhruv.finance.data.tracker.dto.CategoryUpsertDto(
                        name = Category.RESERVED_UNCATEGORISED,
                        kind = "EXPENSE",
                    ),
                )
            }
            if (Category.RESERVED_ADJUSTMENT !in existing) {
                api.createCategory(
                    com.dhruv.finance.data.tracker.dto.CategoryUpsertDto(
                        name = Category.RESERVED_ADJUSTMENT,
                        kind = "EXPENSE",
                        excludedFromSpend = true,
                    ),
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun countTransactionsForCategory(categoryId: String): Result<Int> =
        try {
            val response = api.countTransactionsForCategory("eq.$categoryId")
            val contentRange = response.headers()["Content-Range"]
            val total = contentRange?.substringAfterLast('/')?.toIntOrNull()
            if (total != null) {
                Result.success(total)
            } else {
                Result.failure(IllegalStateException("PostgREST returned no exact count (Content-Range: $contentRange)"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
