package com.dhruv.finance.data.tracker.mapper

import com.dhruv.finance.data.tracker.dto.AccountBalanceDto
import com.dhruv.finance.data.tracker.dto.AccountDto
import com.dhruv.finance.data.tracker.dto.AccountUpsertDto
import com.dhruv.finance.data.tracker.model.Account
import com.dhruv.finance.data.tracker.model.AccountType
import java.time.Instant

fun AccountDto.toDomain(balance: AccountBalanceDto? = null): Account =
    Account(
        id = id,
        name = name,
        type = AccountType.valueOf(type),
        mask = mask,
        isPrimary = isPrimary,
        limitPaise = limitPaise,
        dueDay = dueDay,
        openingBalancePaise = openingBalancePaise,
        reconciledAt = reconciledAt?.let { Instant.parse(it) },
        balancePaise = balance?.balancePaise,
    )

fun Account.toUpsertDto(requestId: String? = null): AccountUpsertDto =
    AccountUpsertDto(
        name = name,
        type = type.name,
        mask = mask,
        isPrimary = isPrimary,
        limitPaise = limitPaise,
        dueDay = dueDay,
        openingBalancePaise = openingBalancePaise,
        requestId = requestId,
    )
