package com.dhruv.finance.data.tracker.mapper

import com.dhruv.finance.data.tracker.dto.RecurringTemplateDto
import com.dhruv.finance.data.tracker.dto.SuggestionDto
import com.dhruv.finance.data.tracker.model.PendingEntry
import com.dhruv.finance.data.tracker.model.RecurringTemplate
import com.dhruv.finance.data.tracker.model.SuggestionStatus
import java.time.Instant
import java.time.LocalDate

fun RecurringTemplateDto.toDomain(): RecurringTemplate =
    RecurringTemplate(
        id = id,
        template = template,
        rrule = rrule,
        nextRun = LocalDate.parse(nextRun),
        amountIsVariable = amountIsVariable,
        paused = paused,
        pausedAt = pausedAt?.let { Instant.parse(it) },
    )

fun SuggestionDto.toDomain(): PendingEntry =
    PendingEntry(
        id = id,
        recurringId = recurringId,
        dueOn = dueOn?.let { LocalDate.parse(it) },
        parsed = parsed,
        status = SuggestionStatus.valueOf(status),
    )
