package com.dhruv.finance.data.tracker.model

/** A holding's ownership direction — maps to `finance.holdings.kind` (BR-C3, frozen). */
enum class HoldingKind { ASSET, LIABILITY }

data class Holding(
    val id: String,
    val name: String,
    val kind: HoldingKind,
    val sector: Sector,
    val investedPaise: Long?,
    val notes: String?,
)

/** Request to [com.dhruv.finance.data.tracker.repo.HoldingRepository.createWithFirstValuation] —
 * bundled into one type rather than a long parameter list (the same shape
 * `finance.create_holding_with_value` itself takes). [sectorCode] is raw/unvalidated input (e.g.
 * from a picker); the repository validates it against [Sector]'s fixed set (NW-BR-004). */
data class CreateHoldingRequest(
    val name: String,
    val kind: HoldingKind,
    val sectorCode: String,
    val valuePaise: Long,
    val asOf: String,
    val investedPaise: Long? = null,
    val notes: String? = null,
    val requestId: String? = null,
)

/** Request to [com.dhruv.finance.data.tracker.repo.HoldingRepository.update] (Phase 9, T052) — a
 * full-value replace of the user-editable fields, never a partial merge. [sectorCode] is validated
 * against [Sector]'s fixed set the same way [CreateHoldingRequest.sectorCode] is. `kind` is not
 * editable (see `UpdateHoldingRequestDto`'s own doc). */
data class UpdateHoldingRequest(
    val name: String,
    val sectorCode: String,
    val investedPaise: Long? = null,
    val notes: String? = null,
)

/**
 * A holding paired with its current (latest, non-deleted) value from `finance.v_latest_valuation`
 * — [currentValuePaise] is null only when no valuation exists yet, which cannot happen for a
 * holding created via [com.dhruv.finance.data.tracker.repo.HoldingRepository.createWithFirstValuation]
 * (BR-C2 guarantees one), but is modelled as nullable because
 * [com.dhruv.finance.data.tracker.repo.HoldingRepository.list] cannot prove that server-side.
 */
data class HoldingWithValue(
    val holding: Holding,
    val currentValuePaise: Long?,
)
