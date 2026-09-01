package com.dhruv.finance.networth

// Sector values are frozen (BR-C3, append-only once shipped) — see
// supabase/schemas/finance/10_tables/holdings.sql and data-model.md's "10 frozen values".
val SectorLabels =
    listOf(
        "BANK" to "Bank",
        "MUTUAL_FUND" to "Mutual fund",
        "STOCKS" to "Stocks",
        "PROPERTY" to "Property",
        "GOLD" to "Gold",
        "EPF_PPF" to "EPF / PPF",
        "CASH" to "Cash",
        "VEHICLE" to "Vehicle",
        "CRYPTO" to "Crypto",
        "OTHER" to "Other",
    )

// Liability type values are frozen (BR-C3) — see liabilities_meta.sql.
val LiabilityTypeLabels =
    listOf(
        "HOME_LOAN" to "Home loan",
        "CAR_LOAN" to "Car loan",
        "CREDIT_CARD" to "Credit card",
        "BNPL" to "Buy now, pay later",
    )

// Trailing window for the Home/C1 net-worth trend, matching v_net_worth_history's fixed 24
// month-end points (data-model.md).
const val NET_WORTH_HISTORY_MONTHS = 24
