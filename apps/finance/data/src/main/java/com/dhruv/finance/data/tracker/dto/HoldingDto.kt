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
