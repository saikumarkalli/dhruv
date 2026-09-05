package com.dhruv.finance.data.tracker.repo

import com.dhruv.finance.data.tracker.dto.MergeCategoriesRequestDto
import com.dhruv.finance.data.tracker.mapper.toDomain
import com.dhruv.finance.data.tracker.mapper.toUpsertDto
import com.dhruv.finance.data.tracker.model.Category
import com.dhruv.finance.data.tracker.net.MoneyApi
import com.dhruv.finance.data.tracker.net.SupabaseClientFactory
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.util.UUID

/** Money tab categories (D8, US5). [mergeCategories] is the ONLY merge path (FR-024, R9) — it
 * calls the `merge_categories` RPC, never a client-side loop. */
interface CategoryRepository {
    suspend fun listCategories(): Result<List<Category>>

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
}
