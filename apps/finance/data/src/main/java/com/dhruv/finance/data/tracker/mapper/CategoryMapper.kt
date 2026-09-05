package com.dhruv.finance.data.tracker.mapper

import com.dhruv.finance.data.tracker.dto.CategoryDto
import com.dhruv.finance.data.tracker.dto.CategorySpendDto
import com.dhruv.finance.data.tracker.dto.CategoryUpsertDto
import com.dhruv.finance.data.tracker.model.Category
import com.dhruv.finance.data.tracker.model.CategoryKind

fun CategoryDto.toDomain(spend: CategorySpendDto? = null): Category =
    Category(
        id = id,
        name = name,
        kind = CategoryKind.valueOf(kind),
        parentId = parentId,
        icon = icon,
        excludedFromSpend = excludedFromSpend,
        spendPaise = spend?.spendPaise,
        sharePercent = spend?.sharePercent,
    )

fun Category.toUpsertDto(requestId: String? = null): CategoryUpsertDto =
    CategoryUpsertDto(
        name = name,
        kind = kind.name,
        parentId = parentId,
        icon = icon,
        excludedFromSpend = excludedFromSpend,
        requestId = requestId,
    )
