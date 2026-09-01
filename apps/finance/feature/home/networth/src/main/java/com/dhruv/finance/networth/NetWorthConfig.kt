package com.dhruv.finance.networth

import androidx.compose.ui.graphics.Color
import com.dhruv.core.ui.theme.DhruvNextColors

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

/** Display label for a raw sector code (falls back to the code itself for forward-compatibility
 * with a sector added server-side before this list is updated — never crashes on an unknown
 * value). */
fun sectorLabel(code: String): String = SectorLabels.firstOrNull { it.first == code }?.second ?: code

/** Cycles the shared chart1-6 palette for an arbitrary number of donut/legend segments — C1's
 * sector breakdown can exceed six sectors, so colors repeat rather than run out. */
fun chartColorForIndex(
    index: Int,
    colors: DhruvNextColors,
): Color {
    val palette = listOf(colors.chart1, colors.chart2, colors.chart3, colors.chart4, colors.chart5, colors.chart6)
    return palette[index % palette.size]
}
