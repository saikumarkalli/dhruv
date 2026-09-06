package com.dhruv.finance.data.tracker.repo

import com.dhruv.finance.data.tracker.dto.CreateLiabilityMetaRequestDto
import com.dhruv.finance.data.tracker.dto.LiabilityMetaDto
import com.dhruv.finance.data.tracker.dto.UpdateLiabilityMetaRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

/** Built off `SupabaseClientFactory.dataRetrofit` — same `Accept-Profile: finance` auto-header
 * convention as [HoldingApi]/[ValuationApi]. `finance.liabilities_meta` has no client DELETE
 * policy (`liabilities_meta.sql`), so this interface defines none — rows only disappear via
 * `public.delete_my_data()`/`public.delete_my_account()` (ADR-0029 decision 5). */
interface LiabilityApi {
    /** `deleted_at is.null` excludes rows removed only via the two erasure functions above. */
    @GET("liabilities_meta")
    suspend fun listAll(
        @Query("deleted_at") notDeleted: String = "is.null",
    ): List<LiabilityMetaDto>

    /** [holdingIdFilter] is a PostgREST filter expression, e.g. `"eq.<uuid>"` — returns 0 or 1
     * rows, same single-row-via-filtered-list convention as [HoldingApi.getById]. */
    @GET("liabilities_meta")
    suspend fun getById(
        @Query("holding_id") holdingIdFilter: String,
        @Query("deleted_at") notDeleted: String = "is.null",
    ): List<LiabilityMetaDto>

    /** `Prefer: return=representation` is required so PostgREST echoes the inserted row back (its
     * default INSERT response body is empty); Moshi needs a body to deserialize. */
    @Headers("Prefer: return=representation")
    @POST("liabilities_meta")
    suspend fun insert(
        @Body body: CreateLiabilityMetaRequestDto,
    ): List<LiabilityMetaDto>

    /** A plain full-value PATCH (never a partial merge) — the only path by which loan terms are
     * ever amended (unlike `valuations`, this table has an UPDATE policy). */
    @Headers("Prefer: return=representation")
    @PATCH("liabilities_meta")
    suspend fun update(
        @Query("holding_id") holdingIdFilter: String,
        @Body body: UpdateLiabilityMetaRequestDto,
    ): List<LiabilityMetaDto>
}
