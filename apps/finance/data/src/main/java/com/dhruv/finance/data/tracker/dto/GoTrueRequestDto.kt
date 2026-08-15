package com.dhruv.finance.data.tracker.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request body for `POST /auth/v1/token?grant_type=id_token` (Google Sign-In).
 *
 * [nonce] is the RAW (unhashed) nonce — GoTrue hashes it itself and compares against the `nonce`
 * claim inside [idToken]'s JWT payload. The value passed to Android Credential Manager's
 * `GetGoogleIdOption.setNonce(...)` must be the SHA-256 hex digest of this same raw value (Google
 * puts the *hashed* nonce in the token; GoTrue hashes what it's given here to match). Required
 * because this project's Supabase instance does not set `external_google_skip_nonce_check` — found
 * live: the id_token exchange fails silently (surfaces to the user as "Couldn't finish signing in")
 * without it.
 */
@JsonClass(generateAdapter = true)
data class GoogleIdTokenRequest(
    @param:Json(name = "provider") val provider: String = "google",
    @param:Json(name = "id_token") val idToken: String,
    @param:Json(name = "nonce") val nonce: String,
)

/** Request body for `POST /auth/v1/token?grant_type=refresh_token`. */
@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
    @param:Json(name = "refresh_token") val refreshToken: String,
)
