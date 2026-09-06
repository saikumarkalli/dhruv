package com.dhruv.finance.data.tracker.model

/** The four frozen valuation sources (BR-C3) — mirrors `finance.valuations`' CHECK constraint. */
enum class ValuationSource {
    MANUAL,
    STATEMENT,
    IMPORT,
    CORRECTION,
    ;

    companion object {
        /** Validates a raw source code, same fixed-set discipline as [Sector.fromCode]. */
        fun fromCode(code: String): ValuationSource? = entries.find { it.name == code }
    }
}

data class Valuation(
    val id: String,
    val holdingId: String,
    val valuePaise: Long,
    val asOf: String,
    val source: ValuationSource,
)

/**
 * One row of a holding's history list (C3) — [deltaPaise]/[deltaPercentBps] are computed against
 * the chronologically-previous (older) valuation, null for the oldest entry (nothing to diff
 * against) or when the previous value was zero ([deltaPercentBps] only — division by zero).
 * Computed in [com.dhruv.finance.data.tracker.repo.ValuationRepository], not the ViewModel, so the
 * history list is ready to render as-is (NW-UI-002). Percent is basis points (Article VII /
 * DAT-BR-008 — floating-point types are forbidden in this module's tracker data path), same
 * convention as `liabilities_meta.rate_bps`; a caller divides by 100 to display it as a percentage.
 */
data class ValuationHistoryEntry(
    val valuation: Valuation,
    val deltaPaise: Long?,
    val deltaPercentBps: Int?,
)
