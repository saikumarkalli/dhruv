package com.dhruv.finance.data.tracker.model

import java.time.Instant
import java.time.LocalDate

/** Domain model for `finance.recurring_templates` (data-model.md "Recurring definition") — a
 * template + schedule that produces [PendingEntry] rows, never [Transaction] rows directly
 * (FR-028, research R7: there is no server scheduler). */
data class RecurringTemplate(
    val id: String,
    val template: Map<String, Any?>,
    val rrule: String,
    val nextRun: LocalDate,
    val amountIsVariable: Boolean,
    val paused: Boolean,
    val pausedAt: Instant?,
)

/** Terminal-once-resolved TEXT enum (constitution Article IX). */
enum class SuggestionStatus { PENDING, ACCEPTED, IGNORED }

/** Domain model for `finance.suggestions` (data-model.md "Pending entry") — a proposed
 * transaction awaiting the user's accept/dismiss; not part of any total until accepted (FR-029). */
data class PendingEntry(
    val id: String,
    val recurringId: String?,
    val dueOn: LocalDate?,
    val parsed: Map<String, Any?>,
    val status: SuggestionStatus,
)
