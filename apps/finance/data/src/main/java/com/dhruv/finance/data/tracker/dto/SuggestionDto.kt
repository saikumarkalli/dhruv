package com.dhruv.finance.data.tracker.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Wire shape of `finance.suggestions` — a pending entry awaiting accept/dismiss (FR-028/FR-029). */
@JsonClass(generateAdapter = true)
data class SuggestionDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "recurring_id") val recurringId: String? = null,
    @param:Json(name = "due_on") val dueOn: String? = null,
    @param:Json(name = "raw_text") val rawText: String? = null,
    @param:Json(name = "parsed") val parsed: Map<String, Any?>,
    @param:Json(name = "status") val status: String = "PENDING",
    @param:Json(name = "created_at") val createdAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class SuggestionUpsertDto(
    @param:Json(name = "recurring_id") val recurringId: String? = null,
    @param:Json(name = "due_on") val dueOn: String? = null,
    @param:Json(name = "parsed") val parsed: Map<String, Any?>,
)

/** Request body for `PATCH suggestions?id=eq.<id>` — accept/dismiss transitions `status` (FR-029). */
@JsonClass(generateAdapter = true)
data class SuggestionStatusDto(
    @param:Json(name = "status") val status: String,
)
