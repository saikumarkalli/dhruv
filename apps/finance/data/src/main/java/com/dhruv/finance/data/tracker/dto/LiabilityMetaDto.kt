package com.dhruv.finance.data.tracker.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Wire shape of a `finance.liabilities_meta` row (ADR-0033). */
@JsonClass(generateAdapter = true)
data class LiabilityMetaDto(
    @param:Json(name = "holding_id") val holdingId: String,
    @param:Json(name = "liability_type") val liabilityType: String,
    @param:Json(name = "rate_bps") val rateBps: Int,
    @param:Json(name = "emi_paise") val emiPaise: Long? = null,
    @param:Json(name = "debit_day") val debitDay: Int? = null,
    @param:Json(name = "tenure_months") val tenureMonths: Int? = null,
    @param:Json(name = "paid_months") val paidMonths: Int = 0,
    @param:Json(name = "original_principal_paise") val originalPrincipalPaise: Long? = null,
    @param:Json(name = "collateral") val collateral: String? = null,
    @param:Json(name = "linked_account_id") val linkedAccountId: String? = null,
)

/** Wire shape of a plain `finance.liabilities_meta` insert — mirrors [LiabilityMetaDto] minus the
 * server-defaulted `paid_months`. */
@JsonClass(generateAdapter = true)
data class CreateLiabilityMetaRequestDto(
    @param:Json(name = "holding_id") val holdingId: String,
    @param:Json(name = "liability_type") val liabilityType: String,
    @param:Json(name = "rate_bps") val rateBps: Int,
    @param:Json(name = "emi_paise") val emiPaise: Long? = null,
    @param:Json(name = "debit_day") val debitDay: Int? = null,
    @param:Json(name = "tenure_months") val tenureMonths: Int? = null,
    @param:Json(name = "original_principal_paise") val originalPrincipalPaise: Long? = null,
    @param:Json(name = "collateral") val collateral: String? = null,
    @param:Json(name = "request_id") val requestId: String? = null,
)

/** Wire shape of a `finance.liabilities_meta` PATCH — every field is the new full value (a plain
 * replace, never a partial merge), matching
 * [com.dhruv.finance.data.tracker.model.UpdateLiabilityMetaRequest]. */
@JsonClass(generateAdapter = true)
data class UpdateLiabilityMetaRequestDto(
    @param:Json(name = "rate_bps") val rateBps: Int,
    @param:Json(name = "emi_paise") val emiPaise: Long? = null,
    @param:Json(name = "debit_day") val debitDay: Int? = null,
    @param:Json(name = "tenure_months") val tenureMonths: Int? = null,
    @param:Json(name = "paid_months") val paidMonths: Int,
    @param:Json(name = "original_principal_paise") val originalPrincipalPaise: Long? = null,
    @param:Json(name = "collateral") val collateral: String? = null,
)
