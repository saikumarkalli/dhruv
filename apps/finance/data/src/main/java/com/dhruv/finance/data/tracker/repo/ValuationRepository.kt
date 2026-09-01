package com.dhruv.finance.data.tracker.repo

import com.dhruv.finance.data.tracker.dto.CorrectValuationRequestDto
import com.dhruv.finance.data.tracker.dto.RecordValuationRequestDto
import com.dhruv.finance.data.tracker.mapper.toDomain
import com.dhruv.finance.data.tracker.model.ValuationHistoryEntry
import com.dhruv.finance.data.tracker.model.ValuationSource
import com.dhruv.finance.data.tracker.net.SupabaseClientFactory
import kotlinx.coroutines.CancellationException

private const val BASIS_POINTS_PER_UNIT = 10_000L

/**
 * A holding's valuation history and mutations (C3/C5, Phase 2 User Stories 2-3). [listHistory]
 * reads `finance.valuations` directly (never `v_latest_valuation`, which carries only the single
 * newest row per holding) and computes each entry's delta against the chronologically-previous
 * value server-round-trip-free, so the screen renders the list as-is (NW-UI-002).
 *
 * [recordValue] and [correctValue] are two distinct write paths, never interchangeable:
 * [recordValue] is a plain append (an ordinary new value); [correctValue] is the only path by
 * which an existing row is ever amended (NW-BR-002/NW-BR-003) — it soft-deletes the wrong row and
 * appends a corrected one in one server-side transaction. Neither ever issues a client-side
 * UPDATE against `value_paise` — `finance.valuations` has no UPDATE policy at all (BR-C1), so
 * there is nothing for a client to call even if it tried.
 */
interface ValuationRepository {
    /** Newest-first (list-display order). */
    suspend fun listHistory(holdingId: String): Result<List<ValuationHistoryEntry>>

    /** [sourceCode] must be a valid [ValuationSource] other than `CORRECTION` — that source is
     * reserved for [correctValue], never user-selectable for an ordinary new value. */
    suspend fun recordValue(
        holdingId: String,
        valuePaise: Long,
        asOf: String,
        sourceCode: String = ValuationSource.MANUAL.name,
        requestId: String? = null,
    ): Result<String>

    /** Soft-deletes [valuationId] and appends the corrected value in one transaction
     * (`finance.correct_valuation`, NW-BR-002/NW-BR-003) — the only path by which a valuation is
     * ever amended. */
    suspend fun correctValue(
        valuationId: String,
        valuePaise: Long,
        asOf: String,
        note: String? = null,
    ): Result<String>
}

class ValuationRepositoryImpl(
    private val valuationApi: ValuationApi,
) : ValuationRepository {
    constructor(
        supabaseClientFactory: SupabaseClientFactory,
    ) : this(supabaseClientFactory.dataRetrofit.create(ValuationApi::class.java))

    @Suppress("TooGenericExceptionCaught")
    override suspend fun listHistory(holdingId: String): Result<List<ValuationHistoryEntry>> =
        try {
            // API returns newest-first already (order=as_of.desc,created_at.desc); the
            // chronologically-previous entry for row i is therefore row i+1.
            val valuations = valuationApi.listHistory(holdingIdFilter = "eq.$holdingId").map { it.toDomain() }
            val entries =
                valuations.mapIndexed { index, valuation ->
                    val previous = valuations.getOrNull(index + 1)
                    val deltaPaise = previous?.let { valuation.valuePaise - it.valuePaise }
                    val deltaPercentBps =
                        if (previous != null && previous.valuePaise != 0L) {
                            ((valuation.valuePaise - previous.valuePaise) * BASIS_POINTS_PER_UNIT / previous.valuePaise).toInt()
                        } else {
                            null
                        }
                    ValuationHistoryEntry(valuation = valuation, deltaPaise = deltaPaise, deltaPercentBps = deltaPercentBps)
                }
            Result.success(entries)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    // Deliberately multiple early returns: sector/source validation, the no-row-returned guard,
    // and the success path are three genuinely distinct exits, same accepted pattern as
    // AuthInterceptor.intercept().
    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    override suspend fun recordValue(
        holdingId: String,
        valuePaise: Long,
        asOf: String,
        sourceCode: String,
        requestId: String?,
    ): Result<String> {
        val source =
            ValuationSource.fromCode(sourceCode)
                ?: return Result.failure(IllegalArgumentException("Unknown valuation source: $sourceCode"))
        if (source == ValuationSource.CORRECTION) {
            return Result.failure(IllegalArgumentException("CORRECTION is written only by correctValue()"))
        }
        return try {
            val inserted =
                valuationApi.insertValuation(
                    RecordValuationRequestDto(
                        holdingId = holdingId,
                        valuePaise = valuePaise,
                        asOf = asOf,
                        source = source.name,
                        requestId = requestId,
                    ),
                )
            val newId =
                inserted.firstOrNull()?.id
                    ?: return Result.failure(IllegalStateException("Insert succeeded but returned no row"))
            Result.success(newId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun correctValue(
        valuationId: String,
        valuePaise: Long,
        asOf: String,
        note: String?,
    ): Result<String> =
        try {
            val newId =
                valuationApi.correctValuation(
                    CorrectValuationRequestDto(
                        valuationId = valuationId,
                        valuePaise = valuePaise,
                        asOf = asOf,
                        note = note,
                    ),
                )
            Result.success(newId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
