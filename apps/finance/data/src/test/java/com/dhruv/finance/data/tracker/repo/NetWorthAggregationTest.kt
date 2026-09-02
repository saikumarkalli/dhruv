package com.dhruv.finance.data.tracker.repo

import com.dhruv.finance.data.tracker.dto.NetWorthBySectorRowDto
import com.dhruv.finance.data.tracker.dto.NetWorthHistoryRowDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeNetWorthApi(
    private val rows: List<NetWorthBySectorRowDto> = emptyList(),
    private val historyRows: List<NetWorthHistoryRowDto> = emptyList(),
) : NetWorthApi {
    var callCount = 0
        private set
    var historyCallCount = 0
        private set

    override suspend fun getNetWorthBySector(): List<NetWorthBySectorRowDto> {
        callCount++
        return rows
    }

    override suspend fun getNetWorthHistory(order: String): List<NetWorthHistoryRowDto> {
        historyCallCount++
        return historyRows
    }
}

class NetWorthAggregationTest {
    // NW-BR-006/BR-C4: total is derived from v_net_worth_by_sector's own per-sector sums, in
    // exactly one read — never a client-side reduction over raw holdings/valuations.
    @Test
    fun `summary total equals assets minus liabilities from the sector view, in one read`() =
        runTest {
            val rows =
                listOf(
                    NetWorthBySectorRowDto(kind = "ASSET", sector = "BANK", holdingCount = 1, valuePaise = 5_00_000_00L),
                    NetWorthBySectorRowDto(kind = "ASSET", sector = "GOLD", holdingCount = 1, valuePaise = 1_00_000_00L),
                    NetWorthBySectorRowDto(kind = "LIABILITY", sector = "OTHER", holdingCount = 1, valuePaise = 2_00_000_00L),
                )
            val api = FakeNetWorthApi(rows)
            val repo: NetWorthRepository = NetWorthRepositoryImpl(api)

            val result = repo.getSummary()

            assertTrue(result.isSuccess)
            val summary = result.getOrNull()!!
            assertEquals(1, api.callCount)
            assertEquals(6_00_000_00L, summary.assetsPaise)
            assertEquals(2_00_000_00L, summary.liabilitiesPaise)
            assertEquals(4_00_000_00L, summary.netPaise)
            assertEquals(3, summary.bySector.size)
        }

    @Test
    fun `empty sector view yields zero net worth, not a failure`() =
        runTest {
            val api = FakeNetWorthApi(emptyList())
            val repo: NetWorthRepository = NetWorthRepositoryImpl(api)

            val summary = repo.getSummary().getOrThrow()

            assertEquals(0L, summary.netPaise)
            assertEquals(0L, summary.assetsPaise)
            assertEquals(0L, summary.liabilitiesPaise)
            assertTrue(summary.bySector.isEmpty())
        }

    // FR-010: the trailing-24-month-end series, oldest-first (as_of.asc) — the caller derives
    // Home's delta from the two newest points, never a client-side re-sort.
    @Test
    fun `getHistory maps every row in the order the API returned`() =
        runTest {
            val rows =
                listOf(
                    NetWorthHistoryRowDto(
                        asOf = "2026-08-01",
                        assetsPaise = 5_00_000_00L,
                        liabilitiesPaise = 1_00_000_00L,
                        netPaise = 4_00_000_00L,
                    ),
                    NetWorthHistoryRowDto(
                        asOf = "2026-08-31",
                        assetsPaise = 5_20_000_00L,
                        liabilitiesPaise = 95_000_00L,
                        netPaise = 4_25_000_00L,
                    ),
                )
            val api = FakeNetWorthApi(historyRows = rows)
            val repo: NetWorthRepository = NetWorthRepositoryImpl(api)

            val result = repo.getHistory()

            assertTrue(result.isSuccess)
            val points = result.getOrNull()!!
            assertEquals(1, api.historyCallCount)
            assertEquals(listOf("2026-08-01", "2026-08-31"), points.map { it.asOf })
            assertEquals(4_25_000_00L, points.last().netPaise)
        }

    @Test
    fun `getHistory succeeds with an empty list, not a failure, when nothing exists yet`() =
        runTest {
            val repo: NetWorthRepository = NetWorthRepositoryImpl(FakeNetWorthApi())

            val result = repo.getHistory()

            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()!!.isEmpty())
        }
}
