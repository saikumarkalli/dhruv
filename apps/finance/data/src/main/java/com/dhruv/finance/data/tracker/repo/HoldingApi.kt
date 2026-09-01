package com.dhruv.finance.data.tracker.repo

import com.dhruv.finance.data.tracker.dto.CorrectValuationRequestDto
import com.dhruv.finance.data.tracker.dto.CreateHoldingWithValueRequestDto
import com.dhruv.finance.data.tracker.dto.HoldingDto
import com.dhruv.finance.data.tracker.dto.LatestValuationRowDto
import com.dhruv.finance.data.tracker.dto.NetWorthBySectorRowDto
import com.dhruv.finance.data.tracker.dto.RecordValuationRequestDto
import com.dhruv.finance.data.tracker.dto.ValuationDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

/** Built off `SupabaseClientFactory.dataRetrofit` — every method here goes through
 * [com.dhruv.finance.data.tracker.net.FinanceSchemaInterceptor], so no `Accept-Profile` header is
 * needed on individual methods. */
interface HoldingApi {
    /** Atomic create (BR-C2/NW-BR-001) — see `finance.create_holding_with_value` (data-model.md).
     * Returns the new holding's id as a bare JSON string. */
    @POST("rpc/create_holding_with_value")
    suspend fun createHoldingWithValue(
        @Body body: CreateHoldingWithValueRequestDto,
    ): String

    /** [kindFilter] is a PostgREST filter expression, e.g. `"eq.ASSET"`. */
    @GET("holdings")
    suspend fun listHoldings(
        @Query("kind") kindFilter: String,
        @Query("deleted_at") notDeleted: String = "is.null",
        @Query("order") order: String = "created_at.desc",
    ): List<HoldingDto>

    /** [idFilter] is a PostgREST filter expression, e.g. `"eq.<uuid>"` — returns 0 or 1 rows;
     * PostgREST has no dedicated single-row-by-id endpoint, only a filtered list. */
    @GET("holdings")
    suspend fun getById(
        @Query("id") idFilter: String,
        @Query("deleted_at") notDeleted: String = "is.null",
    ): List<HoldingDto>
}

/** Read-only access to `finance.v_latest_valuation` (per-holding current value) and the
 * `finance.valuations` table directly (C3's full history — the view only ever has one row per
 * holding, never the history [ValuationRepository.listHistory] needs). */
interface ValuationApi {
    @GET("v_latest_valuation")
    suspend fun listLatestValuations(): List<LatestValuationRowDto>

    /** [holdingIdFilter] is a PostgREST filter expression, e.g. `"eq.<uuid>"`. Newest-first
     * (NW-UI-002); `deleted_at is.null` excludes rows a correction has superseded (BR-C1). */
    @GET("valuations")
    suspend fun listHistory(
        @Query("holding_id") holdingIdFilter: String,
        @Query("deleted_at") notDeleted: String = "is.null",
        @Query("order") order: String = "as_of.desc,created_at.desc",
    ): List<ValuationDto>

    /** A plain append (BR-C1) — an ordinary new value, never an amendment. `Prefer:
     * return=representation` is required so PostgREST echoes the inserted row back (its default
     * INSERT response body is empty); Moshi needs a body to deserialize. */
    @Headers("Prefer: return=representation")
    @POST("valuations")
    suspend fun insertValuation(
        @Body body: RecordValuationRequestDto,
    ): List<ValuationDto>

    /** The only path by which a valuation row is ever amended (NW-BR-002/NW-BR-003) — see
     * `finance.correct_valuation` (data-model.md). Returns the corrected row's new id as a bare
     * JSON string. */
    @POST("rpc/correct_valuation")
    suspend fun correctValuation(
        @Body body: CorrectValuationRequestDto,
    ): String
}

/** Read-only access to `finance.v_net_worth_by_sector` (BR-C4). */
interface NetWorthApi {
    @GET("v_net_worth_by_sector")
    suspend fun getNetWorthBySector(): List<NetWorthBySectorRowDto>
}
