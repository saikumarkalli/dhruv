package com.dhruv.finance.data.tracker.repo

import com.dhruv.finance.data.tracker.dto.CreateHoldingWithValueRequestDto
import com.dhruv.finance.data.tracker.mapper.toDomain
import com.dhruv.finance.data.tracker.model.CreateHoldingRequest
import com.dhruv.finance.data.tracker.model.HoldingKind
import com.dhruv.finance.data.tracker.model.HoldingWithValue
import com.dhruv.finance.data.tracker.model.Sector
import com.dhruv.finance.data.tracker.net.SupabaseClientFactory
import kotlinx.coroutines.CancellationException

/**
 * Holdings CRUD for the net worth tracker (Phase 2, C1-C7). [createWithFirstValuation] is the only
 * creation path — it delegates to the `finance.create_holding_with_value` RPC, which writes the
 * holding and its first valuation in one transaction (BR-C2/NW-BR-001); this repository never
 * issues two separate inserts for a creation, so a failed second write can never leave an orphan
 * holding.
 */
interface HoldingRepository {
    /** [CreateHoldingRequest.sectorCode] is validated against [Sector]'s fixed set before any
     * network call — an unknown code fails locally with [IllegalArgumentException] (NW-BR-004)
     * rather than reaching the server, whose own CHECK constraint would reject it anyway, but only
     * after a round trip. */
    suspend fun createWithFirstValuation(request: CreateHoldingRequest): Result<String>

    /** Holdings of [kind] merged with their current value from `v_latest_valuation` — never a
     * client-side sum over raw valuation history (NFR-8). */
    suspend fun list(kind: HoldingKind): Result<List<HoldingWithValue>>
}

class HoldingRepositoryImpl(
    private val holdingApi: HoldingApi,
    private val valuationApi: ValuationApi,
) : HoldingRepository {
    /** Convenience constructor mirroring `TrackerAccountRepositoryImpl`'s shape — builds both API
     * interfaces off [SupabaseClientFactory.dataRetrofit] (consent-gated) so callers only need to
     * hand this a [SupabaseClientFactory], not expose `retrofit2.Retrofit` to Koin wiring outside
     * this module. */
    constructor(
        supabaseClientFactory: SupabaseClientFactory,
    ) : this(
        supabaseClientFactory.dataRetrofit.create(HoldingApi::class.java),
        supabaseClientFactory.dataRetrofit.create(ValuationApi::class.java),
    )

    @Suppress("TooGenericExceptionCaught")
    override suspend fun createWithFirstValuation(request: CreateHoldingRequest): Result<String> {
        val sector =
            Sector.fromCode(request.sectorCode)
                ?: return Result.failure(IllegalArgumentException("Unknown sector code: ${request.sectorCode}"))
        return try {
            val id =
                holdingApi.createHoldingWithValue(
                    CreateHoldingWithValueRequestDto(
                        name = request.name,
                        kind = request.kind.name,
                        sector = sector.name,
                        valuePaise = request.valuePaise,
                        asOf = request.asOf,
                        investedPaise = request.investedPaise,
                        notes = request.notes,
                        requestId = request.requestId,
                    ),
                )
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun list(kind: HoldingKind): Result<List<HoldingWithValue>> =
        try {
            val holdings = holdingApi.listHoldings(kindFilter = "eq.${kind.name}")
            val latestByHolding = valuationApi.listLatestValuations().associateBy { it.holdingId }
            val result =
                holdings.map { dto ->
                    HoldingWithValue(
                        holding = dto.toDomain(),
                        currentValuePaise = latestByHolding[dto.id]?.valuePaise,
                    )
                }
            Result.success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
