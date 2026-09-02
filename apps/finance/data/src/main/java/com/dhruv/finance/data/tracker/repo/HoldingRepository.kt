package com.dhruv.finance.data.tracker.repo

import com.dhruv.finance.data.tracker.dto.CreateHoldingWithValueRequestDto
import com.dhruv.finance.data.tracker.dto.SoftDeleteHoldingRequestDto
import com.dhruv.finance.data.tracker.dto.UpdateHoldingRequestDto
import com.dhruv.finance.data.tracker.mapper.toDomain
import com.dhruv.finance.data.tracker.model.CreateHoldingRequest
import com.dhruv.finance.data.tracker.model.Holding
import com.dhruv.finance.data.tracker.model.HoldingKind
import com.dhruv.finance.data.tracker.model.HoldingWithValue
import com.dhruv.finance.data.tracker.model.Sector
import com.dhruv.finance.data.tracker.model.UpdateHoldingRequest
import com.dhruv.finance.data.tracker.net.SupabaseClientFactory
import kotlinx.coroutines.CancellationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant


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

    /** A single holding by id (C3's detail screen). Fails with [NoSuchElementException] if the id
     * doesn't exist or doesn't belong to the signed-in user (RLS returns zero rows either way, so
     * the two cases are indistinguishable here — same as everywhere else in this codebase). */
    suspend fun get(holdingId: String): Result<Holding>

    /** C4's edit path (Phase 9, T051/T052) — [UpdateHoldingRequest.sectorCode] is validated the
     * same way [createWithFirstValuation] validates a create. Never touches valuations (BR-C1) or
     * `kind` (not an editable field). */
    suspend fun update(
        holdingId: String,
        request: UpdateHoldingRequest,
    ): Result<Unit>

    /** Soft-delete (Phase 9, T052/T053) — sets `deleted_at` to now, excluding the holding from
     * [list]/[get] without destroying the row (DPDP/undo both need it to still exist). */
    suspend fun softDelete(holdingId: String): Result<Unit>

    /** Undo, within the 5s window a soft-delete's `UndoSnackbarHost` offers (Phase 9, T053) —
     * clears `deleted_at` back to null. There is no durable "Trash" surface beyond that window in
     * this phase (T053's "recoverable location" is deliberately just the undo snackbar itself,
     * recorded as a scope decision rather than left unstated) — a soft-deleted holding is still
     * physically present and could be restored by a future Trash screen, but nothing surfaces it
     * once the undo window closes. */
    suspend fun restore(holdingId: String): Result<Unit>
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

    @Suppress("TooGenericExceptionCaught")
    override suspend fun get(holdingId: String): Result<Holding> =
        try {
            val holding = holdingApi.getById(idFilter = "eq.$holdingId").firstOrNull()
            if (holding == null) {
                Result.failure(NoSuchElementException("No holding with id $holdingId"))
            } else {
                Result.success(holding.toDomain())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun update(
        holdingId: String,
        request: UpdateHoldingRequest,
    ): Result<Unit> {
        val sector =
            Sector.fromCode(request.sectorCode)
                ?: return Result.failure(IllegalArgumentException("Unknown sector code: ${request.sectorCode}"))
        return try {
            holdingApi.updateHolding(
                idFilter = "eq.$holdingId",
                body =
                    UpdateHoldingRequestDto(
                        name = request.name,
                        sector = sector.name,
                        investedPaise = request.investedPaise,
                        notes = request.notes,
                    ),
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun softDelete(holdingId: String): Result<Unit> =
        try {
            holdingApi.softDeleteHolding(
                idFilter = "eq.$holdingId",
                body = SoftDeleteHoldingRequestDto(deletedAt = Instant.now().toString()),
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun restore(holdingId: String): Result<Unit> =
        try {
            holdingApi.restoreHolding(idFilter = "eq.$holdingId", body = RESTORE_BODY)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    private companion object {
        /** See [HoldingApi.restoreHolding]'s doc — a typed DTO cannot express an explicit JSON
         * `null` under this module's Moshi configuration, so this is a raw body shared by every
         * call. Stateless and immutable — safe to share across coroutines/instances. */
        val RESTORE_BODY = "{\"deleted_at\":null}".toRequestBody("application/json; charset=utf-8".toMediaType())
    }
}
