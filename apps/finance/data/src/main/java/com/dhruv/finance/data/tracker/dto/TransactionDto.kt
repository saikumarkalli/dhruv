package com.dhruv.finance.data.tracker.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Wire shape of `finance.transactions` (002-money-tab data-model.md). */
@JsonClass(generateAdapter = true)
data class TransactionDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "type") val type: String,
    @param:Json(name = "amount_paise") val amountPaise: Long,
    @param:Json(name = "account_id") val accountId: String,
    @param:Json(name = "to_account_id") val toAccountId: String? = null,
    @param:Json(name = "category_id") val categoryId: String? = null,
    @param:Json(name = "payee") val payee: String? = null,
    @param:Json(name = "note") val note: String? = null,
    @param:Json(name = "occurred_at") val occurredAt: String,
    @param:Json(name = "cleared") val cleared: Boolean = true,
    @param:Json(name = "receipt_path") val receiptPath: String? = null,
    @param:Json(name = "goal_id") val goalId: String? = null,
    @param:Json(name = "recurring_id") val recurringId: String? = null,
    @param:Json(name = "split_group_id") val splitGroupId: String? = null,
    @param:Json(name = "source") val source: String = "MANUAL",
    @param:Json(name = "request_id") val requestId: String? = null,
    @param:Json(name = "created_at") val createdAt: String? = null,
    @param:Json(name = "deleted_at") val deletedAt: String? = null,
)

/** Request body for creating/editing a transaction. `id`/`created_at` are server-assigned. */
@JsonClass(generateAdapter = true)
data class TransactionUpsertDto(
    @param:Json(name = "type") val type: String,
    @param:Json(name = "amount_paise") val amountPaise: Long,
    @param:Json(name = "account_id") val accountId: String,
    @param:Json(name = "to_account_id") val toAccountId: String? = null,
    @param:Json(name = "category_id") val categoryId: String? = null,
    @param:Json(name = "payee") val payee: String? = null,
    @param:Json(name = "note") val note: String? = null,
    @param:Json(name = "occurred_at") val occurredAt: String,
    @param:Json(name = "cleared") val cleared: Boolean = true,
    @param:Json(name = "receipt_path") val receiptPath: String? = null,
    @param:Json(name = "goal_id") val goalId: String? = null,
    @param:Json(name = "recurring_id") val recurringId: String? = null,
    @param:Json(name = "split_group_id") val splitGroupId: String? = null,
    @param:Json(name = "source") val source: String = "MANUAL",
    @param:Json(name = "request_id") val requestId: String? = null,
)

/** Minimal projection of `finance.transactions` (`select=id`) for a count-only request — see
 * [com.dhruv.finance.data.tracker.net.MoneyApi.countTransactionsForCategory]. A body this narrow
 * would fail Moshi parsing against the full [TransactionDto] (several of its fields are non-null
 * with no default), so this exists purely to let the (otherwise-ignored) response body parse
 * cleanly while [retrofit2.Response.headers]' `Content-Range` carries the actual exact count
 * (FR-024 — the merge confirmation states the exact number of transactions that will move). */
@JsonClass(generateAdapter = true)
data class TransactionCountRowDto(
    @param:Json(name = "id") val id: String,
)

/** Wire shape of `finance.transaction_events` — append-only audit trail (FR-007/FR-008). */
@JsonClass(generateAdapter = true)
data class TransactionEventDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "transaction_id") val transactionId: String,
    @param:Json(name = "at") val at: String,
    @param:Json(name = "kind") val kind: String,
    @param:Json(name = "detail") val detail: Map<String, Any?>? = null,
)

/** Wire shape of `finance.v_month_summary` — server-computed month totals (NFR-8, FR-011). */
@JsonClass(generateAdapter = true)
data class MonthSummaryDto(
    @param:Json(name = "month") val month: String,
    @param:Json(name = "income_paise") val incomePaise: Long,
    @param:Json(name = "expense_paise") val expensePaise: Long,
    @param:Json(name = "excluded_paise") val excludedPaise: Long,
    @param:Json(name = "transfer_paise") val transferPaise: Long,
)
