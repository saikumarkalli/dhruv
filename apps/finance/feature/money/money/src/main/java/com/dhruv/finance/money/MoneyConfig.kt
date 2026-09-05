package com.dhruv.finance.money

/**
 * Screen-level constants for the Money tab (D1-D9) — no-hardcoding rule
 * (constitution Article V / DESIGN-SYSTEM.md). Never inline these in a screen file.
 */
object MoneyConfig {
    /** FR-020: an account balance not reconciled within this window is flagged stale. */
    const val STALENESS_THRESHOLD_DAYS = 30

    /** FR-030 / D9: window for the recurring screen's "next 30 days" list. */
    const val UPCOMING_WINDOW_DAYS = 30

    val accountTypeLabels =
        mapOf(
            "BANK" to "Bank",
            "CASH" to "Cash",
            "WALLET" to "Wallet",
            "CREDIT_CARD" to "Credit card",
        )

    val categoryKindLabels =
        mapOf(
            "EXPENSE" to "Expense",
            "INCOME" to "Income",
        )

    val transactionTypeLabels =
        mapOf(
            "EXPENSE" to "Expense",
            "INCOME" to "Income",
            "TRANSFER" to "Transfer",
        )

    /** Reserved category names seeded per user (data-model.md "Reserved rows"). */
    const val RESERVED_CATEGORY_UNCATEGORISED = "Uncategorised"
    const val RESERVED_CATEGORY_ADJUSTMENT = "Adjustment"
}
