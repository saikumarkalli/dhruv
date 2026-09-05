package com.dhruv.finance.data.tracker.repo

import com.dhruv.finance.data.tracker.mapper.toDomain
import com.dhruv.finance.data.tracker.model.HoldingKind
import com.dhruv.finance.data.tracker.model.NetWorthHistoryPoint
import com.dhruv.finance.data.tracker.model.NetWorthSummary
import com.dhruv.finance.data.tracker.net.SupabaseClientFactory
import kotlinx.coroutines.CancellationException

/**
 * Reads the net-worth total and sector breakdown from `finance.v_net_worth_by_sector`
 * (BR-C4/NW-BR-006) — this repository never sums raw holdings/valuations client-side; the
 * database view is the only place the aggregation happens (NFR-8).
 */
interface NetWorthRepository {
    suspend fun getSummary(): Result<NetWorthSummary>

    /** Oldest-first, trailing 24 month-ends (`finance.v_net_worth_history`, FR-010) — Home's hero
     * delta compares the newest point against the one ~30 days prior (data-model.md), which at this
     * view's month-end granularity is simply the second-to-last point. */
    suspend fun getHistory(): Result<List<NetWorthHistoryPoint>>
}

class NetWorthRepositoryImpl(
    private val netWorthApi: NetWorthApi,
) : NetWorthRepository {
    constructor(
        supabaseClientFactory: SupabaseClientFactory,
    ) : this(supabaseClientFactory.dataRetrofit.create(NetWorthApi::class.java))

    @Suppress("TooGenericExceptionCaught")
    override suspend fun getSummary(): Result<NetWorthSummary> =
        try {
            val bySector = netWorthApi.getNetWorthBySector().map { it.toDomain() }
            val assets = bySector.filter { it.kind == HoldingKind.ASSET }.sumOf { it.valuePaise }
            val liabilities = bySector.filter { it.kind == HoldingKind.LIABILITY }.sumOf { it.valuePaise }
            Result.success(
                NetWorthSummary(
                    netPaise = assets - liabilities,
                    assetsPaise = assets,
                    liabilitiesPaise = liabilities,
                    bySector = bySector,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun getHistory(): Result<List<NetWorthHistoryPoint>> =
        try {
            Result.success(netWorthApi.getNetWorthHistory().map { it.toDomain() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
