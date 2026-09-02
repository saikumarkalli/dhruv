package com.dhruv.finance.data.tracker.mapper

import com.dhruv.finance.data.tracker.dto.LiabilityMetaDto
import com.dhruv.finance.data.tracker.model.LiabilityMeta
import com.dhruv.finance.data.tracker.model.LiabilityType

fun LiabilityMetaDto.toDomain(): LiabilityMeta =
    LiabilityMeta(
        holdingId = holdingId,
        liabilityType = LiabilityType.valueOf(liabilityType),
        rateBps = rateBps,
        emiPaise = emiPaise,
        debitDay = debitDay,
        tenureMonths = tenureMonths,
        paidMonths = paidMonths,
        originalPrincipalPaise = originalPrincipalPaise,
        collateral = collateral,
        linkedAccountId = linkedAccountId,
    )
