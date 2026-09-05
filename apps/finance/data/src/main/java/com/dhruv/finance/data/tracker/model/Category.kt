package com.dhruv.finance.data.tracker.model

/** Append-only TEXT enum (constitution Article IX). */
enum class CategoryKind { EXPENSE, INCOME }

/** Domain model for `finance.categories` (data-model.md "Category"). Identity ([id]) survives
 * rename (FR-023) — only [name] changes on a rename. */
data class Category(
    val id: String,
    val name: String,
    val kind: CategoryKind,
    val parentId: String?,
    val icon: String?,
    val excludedFromSpend: Boolean,
    val spendPaise: Long? = null,
    val sharePercent: Double? = null,
) {
    companion object {
        const val RESERVED_UNCATEGORISED = "Uncategorised"
        const val RESERVED_ADJUSTMENT = "Adjustment"
    }
}
