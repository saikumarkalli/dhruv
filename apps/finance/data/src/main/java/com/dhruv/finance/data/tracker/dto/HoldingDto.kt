package com.dhruv.finance.data.tracker.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Wire shape of a `finance.holdings` row (ADR-0033). */
@JsonClass(generateAdapter = true)
data class HoldingDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "name") val name: String,
    @param:Json(name = "kind") val kind: String,
    @param:Json(name = "sector") val sector: String,
    @param:Json(name = "invested_paise") val investedPaise: Long? = null,
    @param:Json(name = "notes") val notes: String? = null,
)

/** Wire shape of a plain `finance.holdings` PATCH (Phase 9, T052) — a full-value replace of the
 * user-editable fields, never a partial merge, same convention
 * [com.dhruv.finance.data.tracker.dto.UpdateLiabilityMetaRequestDto] already uses. `kind` is
 * deliberately not editable — switching a holding between ASSET and LIABILITY is not a supported
 * edit (no spec/FR covers it; the RLS `holdings_update_own` policy would permit it, but this DTO
 * doesn't expose the field at all, so the client can't attempt it). */
@JsonClass(generateAdapter = true)
data class UpdateHoldingRequestDto(
    @param:Json(name = "name") val name: String,
    @param:Json(name = "sector") val sector: String,
    @param:Json(name = "invested_paise") val investedPaise: Long? = null,
    @param:Json(name = "notes") val notes: String? = null,
)

/** Wire shape of a `finance.holdings` soft-delete PATCH (Phase 9, T052) — [deletedAt] is always a
 * real timestamp here, never null, so Moshi's default null-omission behaviour (it skips a null
 * field entirely rather than writing a JSON `null`, unless the writer's `serializeNulls` is turned
 * on globally) never comes into play. The **restore** direction needs the opposite — an explicit
 * `deleted_at: null` — which this typed DTO cannot produce without that global setting; see
 * [com.dhruv.finance.data.tracker.repo.HoldingApi.restoreHolding]'s raw-JSON-body doc for why it
 * uses [okhttp3.RequestBody] directly instead of a second DTO. */
@JsonClass(generateAdapter = true)
data class SoftDeleteHoldingRequestDto(
    @param:Json(name = "deleted_at") val deletedAt: String,
)

/** Wire shape of a `finance.create_holding_with_value(...)` RPC request body (data-model.md). */
@JsonClass(generateAdapter = true)
data class CreateHoldingWithValueRequestDto(
    @param:Json(name = "p_name") val name: String,
    @param:Json(name = "p_kind") val kind: String,
    @param:Json(name = "p_sector") val sector: String,
    @param:Json(name = "p_value_paise") val valuePaise: Long,
    @param:Json(name = "p_as_of") val asOf: String,
    @param:Json(name = "p_source") val source: String = "MANUAL",
    @param:Json(name = "p_invested_paise") val investedPaise: Long? = null,
    @param:Json(name = "p_notes") val notes: String? = null,
    @param:Json(name = "p_request_id") val requestId: String? = null,
)

/** Wire shape of a `finance.v_latest_valuation` row — only the columns
 * [com.dhruv.finance.data.tracker.repo.HoldingRepository.list] actually needs; Moshi's generated
 * adapter skips the view's other columns rather than erroring on them. */
@JsonClass(generateAdapter = true)
data class LatestValuationRowDto(
    @param:Json(name = "holding_id") val holdingId: String,
    @param:Json(name = "value_paise") val valuePaise: Long,
)

/** Wire shape of a `finance.v_net_worth_by_sector` row (data-model.md). */
@JsonClass(generateAdapter = true)
data class NetWorthBySectorRowDto(
    @param:Json(name = "kind") val kind: String,
    @param:Json(name = "sector") val sector: String,
    @param:Json(name = "holding_count") val holdingCount: Int,
    @param:Json(name = "value_paise") val valuePaise: Long,
)

/** Wire shape of a `finance.v_net_worth_history` row (data-model.md) — one of the trailing 24
 * month-end points behind Home's hero delta/sparkline (FR-010). */
@JsonClass(generateAdapter = true)
data class NetWorthHistoryRowDto(
    @param:Json(name = "as_of") val asOf: String,
    @param:Json(name = "assets_paise") val assetsPaise: Long,
    @param:Json(name = "liabilities_paise") val liabilitiesPaise: Long,
    @param:Json(name = "net_paise") val netPaise: Long,
)

/** Wire shape of a full `finance.valuations` row (C3's history list — every column, unlike
 * [LatestValuationRowDto]'s two-column projection off the latest-only view). */
@JsonClass(generateAdapter = true)
data class ValuationDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "holding_id") val holdingId: String,
    @param:Json(name = "value_paise") val valuePaise: Long,
    @param:Json(name = "as_of") val asOf: String,
    @param:Json(name = "source") val source: String,
)

/** Wire shape of a plain (non-RPC) `finance.valuations` insert — an ordinary new value
 * (BR-C1's append path, never a correction; [source] must not be `"CORRECTION"`, which is
 * reserved for [CorrectValuationRequestDto]'s RPC). */
@JsonClass(generateAdapter = true)
data class RecordValuationRequestDto(
    @param:Json(name = "holding_id") val holdingId: String,
    @param:Json(name = "value_paise") val valuePaise: Long,
    @param:Json(name = "as_of") val asOf: String,
    @param:Json(name = "source") val source: String,
    @param:Json(name = "request_id") val requestId: String? = null,
)

/** Wire shape of a `finance.correct_valuation(...)` RPC request body (data-model.md) — the only
 * path by which a valuation row is ever amended (NW-BR-002/NW-BR-003). */
@JsonClass(generateAdapter = true)
data class CorrectValuationRequestDto(
    @param:Json(name = "p_valuation_id") val valuationId: String,
    @param:Json(name = "p_value_paise") val valuePaise: Long,
    @param:Json(name = "p_as_of") val asOf: String,
    @param:Json(name = "p_note") val note: String? = null,
)
