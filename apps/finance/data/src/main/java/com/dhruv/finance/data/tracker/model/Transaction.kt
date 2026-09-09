package com.dhruv.finance.data.tracker.model

import java.time.Instant

/** Append-only TEXT enum (constitution Article IX). Sign comes from [type], never from the
 * stored `amount_paise` value (Article VII) — [Transaction.signedAmountPaise] applies it. */
enum class TransactionType { EXPENSE, INCOME, TRANSFER }

/** Append-only TEXT enum — where a transaction came from. `SMS`/`IMPORT` are unused until Phase 7. */
enum class TransactionSource { MANUAL, RECURRING, RECONCILE, SMS, IMPORT }

/** Domain model for `finance.transactions` (data-model.md "Transaction"). [amountPaise] is always
 * positive on the wire (DB CHECK) — [signedAmountPaise] is the presentation-ready signed value. */
data class Transaction(
    val id: String,
    val type: TransactionType,
    val amountPaise: Long,
    val accountId: String,
    val toAccountId: String?,
    val categoryId: String?,
    val payee: String?,
    val note: String?,
    val occurredAt: Instant,
    val cleared: Boolean,
    val receiptPath: String?,
    val goalId: String?,
    val recurringId: String?,
    val splitGroupId: String?,
    val source: TransactionSource,
) {
    /** EXPENSE/TRANSFER-out is negative, INCOME/TRANSFER-in is positive — this is the "from
     * account_id's point of view" sign; a TRANSFER's destination side is a separate row's concern
     * in `v_account_balances`, not this model's. */
    val signedAmountPaise: Long
        get() = if (type == TransactionType.INCOME) amountPaise else -amountPaise
}

/** Append-only TEXT enum — every value [finance.fn_transaction_audit] can write (data-model.md
 * "Transaction history entry"). */
enum class TransactionEventKind {
    CREATED,
    EDITED,
    CATEGORY_CHANGED,
    DELETED,
    ACCEPTED_FROM_RECURRING,
    RECONCILED,
}

/** Domain model for `finance.transaction_events` — append-only, database-enforced (FR-008). */
data class TransactionEvent(
    val id: String,
    val transactionId: String,
    val at: Instant,
    val kind: TransactionEventKind,
    val detail: Map<String, Any?>?,
)

/** Server-computed month totals from `finance.v_month_summary` (NFR-8, FR-011). */
data class MonthSummary(
    val month: String,
    val incomePaise: Long,
    val expensePaise: Long,
    val excludedPaise: Long,
    val transferPaise: Long,
) {
    val savedPercent: Int
        get() = if (incomePaise <= 0) 0 else (((incomePaise - expensePaise) * 100) / incomePaise).toInt()
}
