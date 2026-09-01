package com.dhruv.finance.data.tracker.mapper

import com.dhruv.finance.data.tracker.dto.HoldingDto
import com.dhruv.finance.data.tracker.dto.NetWorthBySectorRowDto
import com.dhruv.finance.data.tracker.dto.ValuationDto
import com.dhruv.finance.data.tracker.model.Holding
import com.dhruv.finance.data.tracker.model.HoldingKind
import com.dhruv.finance.data.tracker.model.Sector
import com.dhruv.finance.data.tracker.model.SectorBreakdown
import com.dhruv.finance.data.tracker.model.Valuation
import com.dhruv.finance.data.tracker.model.ValuationSource

fun HoldingDto.toDomain(): Holding =
    Holding(
        id = id,
        name = name,
        kind = HoldingKind.valueOf(kind),
        sector = Sector.valueOf(sector),
        investedPaise = investedPaise,
        notes = notes,
    )

fun NetWorthBySectorRowDto.toDomain(): SectorBreakdown =
    SectorBreakdown(
        kind = HoldingKind.valueOf(kind),
        sector = Sector.valueOf(sector),
        holdingCount = holdingCount,
        valuePaise = valuePaise,
    )

fun ValuationDto.toDomain(): Valuation =
    Valuation(
        id = id,
        holdingId = holdingId,
        valuePaise = valuePaise,
        asOf = asOf,
        source = ValuationSource.valueOf(source),
    )
