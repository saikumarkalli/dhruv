package com.dhruv.finance.data.tracker.model

data class SectorBreakdown(
    val kind: HoldingKind,
    val sector: Sector,
    val holdingCount: Int,
    val valuePaise: Long,
)

/**
 * BR-C4: net = Σ latest asset valuations − Σ latest liability outstandings. [bySector] is read
 * straight from `finance.v_net_worth_by_sector`, and [netPaise]/[assetsPaise]/[liabilitiesPaise]
 * are sums over that same list — never a client-side reduction over raw holdings/valuations
 * (NW-BR-006, NFR-8).
 */
data class NetWorthSummary(
    val netPaise: Long,
    val assetsPaise: Long,
    val liabilitiesPaise: Long,
    val bySector: List<SectorBreakdown>,
)
