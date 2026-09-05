package com.dhruv.finance.data.tracker.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Wire shape of `finance.accounts` (002-money-tab data-model.md). */
@JsonClass(generateAdapter = true)
data class AccountDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "name") val name: String,
    @param:Json(name = "type") val type: String,
    @param:Json(name = "mask") val mask: String? = null,
    @param:Json(name = "is_primary") val isPrimary: Boolean = false,
    @param:Json(name = "limit_paise") val limitPaise: Long? = null,
    @param:Json(name = "due_day") val dueDay: Int? = null,
    @param:Json(name = "opening_balance_paise") val openingBalancePaise: Long,
    @param:Json(name = "reconciled_at") val reconciledAt: String? = null,
    @param:Json(name = "request_id") val requestId: String? = null,
    @param:Json(name = "created_at") val createdAt: String? = null,
    @param:Json(name = "deleted_at") val deletedAt: String? = null,
)

/** Wire shape of `finance.v_account_balances` — server-computed current balance (NFR-8). */
@JsonClass(generateAdapter = true)
data class AccountBalanceDto(
    @param:Json(name = "account_id") val accountId: String,
    @param:Json(name = "type") val type: String,
    @param:Json(name = "counts_as_spendable") val countsAsSpendable: Boolean,
    @param:Json(name = "balance_paise") val balancePaise: Long,
)

/** Request body for creating/updating an account. Only the fields a client may set. */
@JsonClass(generateAdapter = true)
data class AccountUpsertDto(
    @param:Json(name = "name") val name: String,
    @param:Json(name = "type") val type: String,
    @param:Json(name = "mask") val mask: String? = null,
    @param:Json(name = "is_primary") val isPrimary: Boolean = false,
    @param:Json(name = "limit_paise") val limitPaise: Long? = null,
    @param:Json(name = "due_day") val dueDay: Int? = null,
    @param:Json(name = "opening_balance_paise") val openingBalancePaise: Long = 0,
    @param:Json(name = "request_id") val requestId: String? = null,
)
