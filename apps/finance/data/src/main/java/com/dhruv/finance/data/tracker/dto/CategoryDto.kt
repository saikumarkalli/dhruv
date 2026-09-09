package com.dhruv.finance.data.tracker.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Wire shape of `finance.categories` (002-money-tab data-model.md). */
@JsonClass(generateAdapter = true)
data class CategoryDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "name") val name: String,
    @param:Json(name = "kind") val kind: String,
    @param:Json(name = "parent_id") val parentId: String? = null,
    @param:Json(name = "icon") val icon: String? = null,
    @param:Json(name = "excluded_from_spend") val excludedFromSpend: Boolean = false,
    @param:Json(name = "request_id") val requestId: String? = null,
    @param:Json(name = "created_at") val createdAt: String? = null,
    @param:Json(name = "deleted_at") val deletedAt: String? = null,
)

/** Wire shape of `finance.v_category_spend` — server-computed month spend + share (NFR-8).
 * [sharePercentTenths] is an integer count of tenths-of-a-percent (425 = 42.5%) — constitution
 * Article VII / DAT-BR-008 forbids floating-point numeric types anywhere under the tracker package,
 * even for a non-money percentage, enforced by `checkTrackerMoneyPrecision`. */
@JsonClass(generateAdapter = true)
data class CategorySpendDto(
    @param:Json(name = "month") val month: String,
    @param:Json(name = "category_id") val categoryId: String,
    @param:Json(name = "category_name") val categoryName: String,
    @param:Json(name = "category_kind") val categoryKind: String,
    @param:Json(name = "excluded_from_spend") val excludedFromSpend: Boolean,
    @param:Json(name = "spend_paise") val spendPaise: Long,
    @param:Json(name = "share_percent_tenths") val sharePercentTenths: Int,
)

@JsonClass(generateAdapter = true)
data class CategoryUpsertDto(
    @param:Json(name = "name") val name: String,
    @param:Json(name = "kind") val kind: String,
    @param:Json(name = "parent_id") val parentId: String? = null,
    @param:Json(name = "icon") val icon: String? = null,
    @param:Json(name = "excluded_from_spend") val excludedFromSpend: Boolean = false,
    @param:Json(name = "request_id") val requestId: String? = null,
)

/** Request body for `PATCH categories?id=eq.<id>` — a rename touches only `name` (FR-023). */
@JsonClass(generateAdapter = true)
data class CategoryRenameDto(
    @param:Json(name = "name") val name: String,
)

/** Request body for `rpc/merge_categories`. */
@JsonClass(generateAdapter = true)
data class MergeCategoriesRequestDto(
    @param:Json(name = "p_source") val source: String,
    @param:Json(name = "p_target") val target: String,
)
