package com.dhruv.finance.data.tracker.model

/**
 * The ten frozen sector values (BR-C3, append-only once shipped) — mirrors the CHECK constraint in
 * `supabase/schemas/finance/10_tables/holdings.sql`, the single other place this set is spelled
 * out. Never rename a shipped constant; add new ones only.
 */
enum class Sector {
    BANK,
    MUTUAL_FUND,
    STOCKS,
    PROPERTY,
    GOLD,
    EPF_PPF,
    CASH,
    VEHICLE,
    CRYPTO,
    OTHER,
    ;

    companion object {
        /** Validates a raw sector code (e.g. from a picker) against the fixed set (NW-BR-004) —
         * never accepts free text. Returns null for anything outside the ten values above. */
        fun fromCode(code: String): Sector? = entries.find { it.name == code }
    }
}
