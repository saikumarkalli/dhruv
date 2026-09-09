package com.dhruv.finance.data.tracker.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Wire shape of `finance.recurring_templates` (002-money-tab data-model.md). */
@JsonClass(generateAdapter = true)
data class RecurringTemplateDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "template") val template: Map<String, Any?>,
    @param:Json(name = "rrule") val rrule: String,
    @param:Json(name = "next_run") val nextRun: String,
    @param:Json(name = "amount_is_variable") val amountIsVariable: Boolean = false,
    @param:Json(name = "paused") val paused: Boolean = false,
    @param:Json(name = "paused_at") val pausedAt: String? = null,
    @param:Json(name = "request_id") val requestId: String? = null,
    @param:Json(name = "created_at") val createdAt: String? = null,
    @param:Json(name = "deleted_at") val deletedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class RecurringTemplateUpsertDto(
    @param:Json(name = "template") val template: Map<String, Any?>,
    @param:Json(name = "rrule") val rrule: String,
    @param:Json(name = "next_run") val nextRun: String,
    @param:Json(name = "amount_is_variable") val amountIsVariable: Boolean = false,
    @param:Json(name = "request_id") val requestId: String? = null,
)

/** Request body for `PATCH recurring_templates?id=eq.<id>` — pause/resume (FR-031). */
@JsonClass(generateAdapter = true)
data class RecurringPauseDto(
    @param:Json(name = "paused") val paused: Boolean,
    @param:Json(name = "paused_at") val pausedAt: String?,
)

/** Request body for `PATCH recurring_templates?id=eq.<id>` — full edit (FR-031a): amount,
 * category, account and schedule. Deliberately excludes `request_id` — that column identifies the
 * original create, and an edit must not disturb it. */
@JsonClass(generateAdapter = true)
data class RecurringTemplateEditDto(
    @param:Json(name = "template") val template: Map<String, Any?>,
    @param:Json(name = "rrule") val rrule: String,
    @param:Json(name = "next_run") val nextRun: String,
    @param:Json(name = "amount_is_variable") val amountIsVariable: Boolean = false,
)
