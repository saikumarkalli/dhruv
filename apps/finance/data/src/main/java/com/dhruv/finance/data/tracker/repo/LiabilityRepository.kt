package com.dhruv.finance.data.tracker.repo

import com.dhruv.finance.data.tracker.dto.CreateLiabilityMetaRequestDto
import com.dhruv.finance.data.tracker.dto.UpdateLiabilityMetaRequestDto
import com.dhruv.finance.data.tracker.mapper.toDomain
import com.dhruv.finance.data.tracker.model.CreateLiabilityMetaRequest
import com.dhruv.finance.data.tracker.model.LiabilityMeta
import com.dhruv.finance.data.tracker.model.LiabilityType
import com.dhruv.finance.data.tracker.model.UpdateLiabilityMetaRequest
import com.dhruv.finance.data.tracker.net.SupabaseClientFactory
import kotlinx.coroutines.CancellationException

/**
 * `finance.liabilities_meta` CRUD (Phase 2 User Story 4, C6/C7) — loan/card terms for a liability
 * holding. Unlike [ValuationRepository], this table is mutable (`liabilities_meta.sql`'s own
 * comment: loan terms genuinely change), so [updateMeta] issues a plain PATCH — never routed
 * through an RPC, since no atomic multi-table write is involved the way holding creation is
 * (BR-C2). [createMeta] is a second call after [HoldingRepository.createWithFirstValuation]
 * succeeds, not a single atomic transaction — a liability's loan terms are optional metadata on
 * top of the holding+valuation pair that BR-C2 already guarantees, not a third leg of that same
 * guarantee.
 */
interface LiabilityRepository {
    /** [CreateLiabilityMetaRequest.liabilityTypeCode] is validated against [LiabilityType]'s fixed
     * set before any network call — an unknown code fails locally with [IllegalArgumentException]
     * (same discipline as [HoldingRepository.createWithFirstValuation] for `Sector`). */
    suspend fun createMeta(request: CreateLiabilityMetaRequest): Result<Unit>

    /** Every liability's terms, for C6's grouped-by-type overview — merged client-side against
     * [HoldingRepository.list]'s `LIABILITY`-kind holdings by [LiabilityMeta.holdingId]. */
    suspend fun listAll(): Result<List<LiabilityMeta>>

    /** A single liability's terms (C7's detail screen). Fails with [NoSuchElementException] if
     * [holdingId] has no `liabilities_meta` row (same not-found convention as
     * [HoldingRepository.get]). */
    suspend fun get(holdingId: String): Result<LiabilityMeta>

    suspend fun updateMeta(
        holdingId: String,
        request: UpdateLiabilityMetaRequest,
    ): Result<Unit>
}

class LiabilityRepositoryImpl(
    private val liabilityApi: LiabilityApi,
) : LiabilityRepository {
    /** Convenience constructor mirroring `HoldingRepositoryImpl`'s shape. */
    constructor(
        supabaseClientFactory: SupabaseClientFactory,
    ) : this(supabaseClientFactory.dataRetrofit.create(LiabilityApi::class.java))

    @Suppress("TooGenericExceptionCaught")
    override suspend fun createMeta(request: CreateLiabilityMetaRequest): Result<Unit> {
        val type =
            LiabilityType.fromCode(request.liabilityTypeCode)
                ?: return Result.failure(IllegalArgumentException("Unknown liability type: ${request.liabilityTypeCode}"))
        return try {
            liabilityApi.insert(
                CreateLiabilityMetaRequestDto(
                    holdingId = request.holdingId,
                    liabilityType = type.name,
                    rateBps = request.rateBps,
                    emiPaise = request.emiPaise,
                    debitDay = request.debitDay,
                    tenureMonths = request.tenureMonths,
                    originalPrincipalPaise = request.originalPrincipalPaise,
                    collateral = request.collateral,
                    requestId = request.requestId,
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
    override suspend fun listAll(): Result<List<LiabilityMeta>> =
        try {
            Result.success(liabilityApi.listAll().map { it.toDomain() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun get(holdingId: String): Result<LiabilityMeta> =
        try {
            val meta = liabilityApi.getById(holdingIdFilter = "eq.$holdingId").firstOrNull()
            if (meta == null) {
                Result.failure(NoSuchElementException("No liability terms for holding $holdingId"))
            } else {
                Result.success(meta.toDomain())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun updateMeta(
        holdingId: String,
        request: UpdateLiabilityMetaRequest,
    ): Result<Unit> =
        try {
            liabilityApi.update(
                holdingIdFilter = "eq.$holdingId",
                body =
                    UpdateLiabilityMetaRequestDto(
                        rateBps = request.rateBps,
                        emiPaise = request.emiPaise,
                        debitDay = request.debitDay,
                        tenureMonths = request.tenureMonths,
                        paidMonths = request.paidMonths,
                        originalPrincipalPaise = request.originalPrincipalPaise,
                        collateral = request.collateral,
                    ),
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
