package com.dhruv.finance.data.tracker.model

import java.time.Instant

/** `type` is a frozen, append-only TEXT enum at the DB layer (constitution Article IX) —
 * [AccountType.name] is the wire value, never renamed once shipped. */
enum class AccountType { BANK, CASH, WALLET, CREDIT_CARD }

/** Domain model for `finance.accounts` (data-model.md "Account"). [balancePaise] is read from
 * `finance.v_account_balances`, never summed client-side (NFR-8) — null until that read completes. */
data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val mask: String?,
    val isPrimary: Boolean,
    val limitPaise: Long?,
    val dueDay: Int?,
    val openingBalancePaise: Long,
    val reconciledAt: Instant?,
    val balancePaise: Long? = null,
) {
    /** FR-017: bank/cash/wallet balances count toward "spendable now"; credit does not. */
    val countsAsSpendable: Boolean get() = type != AccountType.CREDIT_CARD
}
