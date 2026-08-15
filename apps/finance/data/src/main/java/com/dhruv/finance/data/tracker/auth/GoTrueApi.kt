package com.dhruv.finance.data.tracker.auth

import com.dhruv.finance.data.tracker.dto.GoTrueSessionDto
import com.dhruv.finance.data.tracker.dto.GoogleIdTokenRequest
import com.dhruv.finance.data.tracker.dto.RefreshTokenRequest
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

/** Supabase GoTrue auth endpoints (ADR-0029). Built off `SupabaseClientFactory`'s `authClient`. */
interface GoTrueApi {
    @POST("token")
    suspend fun signInWithIdToken(
        @Query("grant_type") grantType: String = "id_token",
        @Body body: GoogleIdTokenRequest,
    ): GoTrueSessionDto

    @POST("token")
    suspend fun refresh(
        @Query("grant_type") grantType: String = "refresh_token",
        @Body body: RefreshTokenRequest,
    ): GoTrueSessionDto
}
