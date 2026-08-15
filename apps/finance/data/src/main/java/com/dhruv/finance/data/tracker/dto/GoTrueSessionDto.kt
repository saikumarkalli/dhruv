package com.dhruv.finance.data.tracker.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Wire shape of Supabase GoTrue's `/auth/v1/token` response (both the initial `id_token`
 * sign-in and the `refresh_token` grant return this same shape). ADR-0029.
 *
 * `expiresAt` is a Unix timestamp in **seconds** (GoTrue convention), not millis.
 */
@JsonClass(generateAdapter = true)
data class GoTrueSessionDto(
    @param:Json(name = "access_token") val accessToken: String,
    @param:Json(name = "token_type") val tokenType: String,
    @param:Json(name = "expires_in") val expiresIn: Long,
    @param:Json(name = "expires_at") val expiresAt: Long,
    @param:Json(name = "refresh_token") val refreshToken: String,
    @param:Json(name = "user") val user: GoTrueUserDto,
)

@JsonClass(generateAdapter = true)
data class GoTrueUserDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "email") val email: String?,
    @param:Json(name = "user_metadata") val userMetadata: GoTrueUserMetadataDto? = null,
)

/**
 * The subset of Google's profile claims GoTrue promotes into `user_metadata` — verified against a
 * real sign-in's actual stored row (`auth.users.raw_user_meta_data`), not assumed from generic
 * OAuth docs: Google populates both `name`/`full_name` (identical value in practice) and both
 * `picture`/`avatar_url` (also identical) — [displayName]/[avatarUrl] below pick a single value
 * per concern rather than exposing both raw duplicate fields to callers.
 */
@JsonClass(generateAdapter = true)
data class GoTrueUserMetadataDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "full_name") val fullName: String? = null,
    @param:Json(name = "picture") val picture: String? = null,
    @param:Json(name = "avatar_url") val avatarUrl: String? = null,
) {
    val displayName: String? get() = fullName ?: name
    val resolvedAvatarUrl: String? get() = avatarUrl ?: picture
}
