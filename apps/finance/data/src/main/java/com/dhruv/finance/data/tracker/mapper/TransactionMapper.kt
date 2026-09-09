package com.dhruv.finance.data.tracker.mapper

import com.dhruv.finance.data.tracker.dto.MonthSummaryDto
import com.dhruv.finance.data.tracker.dto.TransactionDto
import com.dhruv.finance.data.tracker.dto.TransactionEventDto
import com.dhruv.finance.data.tracker.dto.TransactionUpsertDto
import com.dhruv.finance.data.tracker.model.MonthSummary
import com.dhruv.finance.data.tracker.model.Transaction
import com.dhruv.finance.data.tracker.model.TransactionEvent
import com.dhruv.finance.data.tracker.model.TransactionEventKind
import com.dhruv.finance.data.tracker.model.TransactionSource
import com.dhruv.finance.data.tracker.model.TransactionType
import java.time.Instant

fun TransactionDto.toDomain(): Transaction =
    Transaction(
        id = id,
        type = TransactionType.valueOf(type),
        amountPaise = amountPaise,
        accountId = accountId,
        toAccountId = toAccountId,
        categoryId = categoryId,
        payee = payee,
        note = note,
        occurredAt = Instant.parse(occurredAt),
        cleared = cleared,
        receiptPath = receiptPath,
        goalId = goalId,
        recurringId = recurringId,
        splitGroupId = splitGroupId,
        source = TransactionSource.valueOf(source),
    )

fun Transaction.toUpsertDto(requestId: String? = null): TransactionUpsertDto =
    TransactionUpsertDto(
        type = type.name,
        amountPaise = amountPaise,
        accountId = accountId,
        toAccountId = toAccountId,
        categoryId = categoryId,
        payee = payee,
        note = note,
        occurredAt = occurredAt.toString(),
        cleared = cleared,
        receiptPath = receiptPath,
        goalId = goalId,
        recurringId = recurringId,
        splitGroupId = splitGroupId,
        source = source.name,
        requestId = requestId,
    )

fun TransactionEventDto.toDomain(): TransactionEvent =
    TransactionEvent(
        id = id,
        transactionId = transactionId,
        at = Instant.parse(at),
        kind = TransactionEventKind.valueOf(kind),
        detail = detail,
    )

fun MonthSummaryDto.toDomain(): MonthSummary =
    MonthSummary(
        month = month,
        incomePaise = incomePaise,
        expensePaise = expensePaise,
        excludedPaise = excludedPaise,
        transferPaise = transferPaise,
    )
