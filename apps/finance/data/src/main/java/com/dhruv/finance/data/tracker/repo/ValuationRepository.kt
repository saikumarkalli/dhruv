package com.dhruv.finance.data.tracker.repo

import com.dhruv.finance.data.tracker.mapper.toDomain
import com.dhruv.finance.data.tracker.model.ValuationHistoryEntry
import com.dhruv.finance.data.tracker.net.SupabaseClientFactory
import kotlinx.coroutines.CancellationException

private const val BASIS_POINTS_PER_UNIT = 10_000L

/**
 * A holding's valuation history (C3, Phase 2 User Story 2). [listHistory] reads
 * `finance.valuations` directly (never `v_latest_valuation`, which carries only the single newest
 * row per holding) and computes each entry's delta against the chronologically-previous value
 * server-round-trip-free, so the screen renders the list as-is (NW-UI-002).
 *
 * `recordValue()`/`correctValue()` (Phase 2 User Story 3, BR-C1/NW-BR-002/NW-BR-003) land in this
 * same file/interface in that later phase — this phase only needs the read path.
 */
interface ValuationRepository {
    /** Newest-first (list-display order). */
    suspend fun listHistory(holdingId: String): Result<List<ValuationHistoryEntry>>
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
}
