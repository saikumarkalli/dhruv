package com.dhruv.finance.data.tracker.repo

import com.dhruv.finance.data.tracker.dto.NetWorthBySectorRowDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeNetWorthApi(
    private val rows: List<NetWorthBySectorRowDto>,
) : NetWorthApi {
    var callCount = 0
        private set

    override suspend fun getNetWorthBySector(): List<NetWorthBySectorRowDto> {
        callCount++
        return rows
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
}
