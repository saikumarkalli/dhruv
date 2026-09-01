package com.dhruv.finance.data.tracker.repo

import com.dhruv.finance.data.tracker.dto.ValuationDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeHistoryValuationApi(
    private val rows: List<ValuationDto>,
) : ValuationApi {
    override suspend fun listLatestValuations(): List<com.dhruv.finance.data.tracker.dto.LatestValuationRowDto> =
        throw UnsupportedOperationException("not exercised by this test")

    override suspend fun listHistory(
        holdingIdFilter: String,
        notDeleted: String,
        order: String,
    ): List<ValuationDto> = rows
}

class ValuationRepositoryTest {
    // NW-UI-002: entries render newest-first, each carrying its delta vs the chronologically-
    // previous (older) entry; the API already returns newest-first (order=as_of.desc), so the
    // repository must not re-sort — it trusts that order and diffs adjacent rows.
    @Test
    fun `history is newest-first with each entry's delta computed against the previous value`() =
        runTest {
            val rows =
                listOf(
                    ValuationDto("v3", "h1", 5_00_000_00L, "2026-08-01", "MANUAL"),
                    ValuationDto("v2", "h1", 4_50_000_00L, "2026-07-01", "MANUAL"),
                    ValuationDto("v1", "h1", 4_00_000_00L, "2026-06-01", "MANUAL"),
                )
            val repo: ValuationRepository = ValuationRepositoryImpl(FakeHistoryValuationApi(rows))

            val result = repo.listHistory("h1")

            assertTrue(result.isSuccess)
            val entries = result.getOrNull()!!
            assertEquals(3, entries.size)
            assertEquals(listOf("v3", "v2", "v1"), entries.map { it.valuation.id })

            // Newest (v3) vs previous (v2): +50,000.00 (1111 bps = 11.11%)
            assertEquals(50_000_00L, entries[0].deltaPaise)
            assertEquals(1111, entries[0].deltaPercentBps)
            // v2 vs v1: +50,000.00 (1250 bps = 12.5%)
            assertEquals(50_000_00L, entries[1].deltaPaise)
            assertEquals(1250, entries[1].deltaPercentBps)
            // Oldest entry has nothing to diff against.
            assertNull(entries[2].deltaPaise)
            assertNull(entries[2].deltaPercentBps)
        }

    @Test
    fun `a single entry has no delta`() =
        runTest {
            val rows = listOf(ValuationDto("v1", "h1", 1_00_000_00L, "2026-06-01", "MANUAL"))
            val repo: ValuationRepository = ValuationRepositoryImpl(FakeHistoryValuationApi(rows))

            val entries = repo.listHistory("h1").getOrThrow()

            assertEquals(1, entries.size)
            assertNull(entries[0].deltaPaise)
        }

    @Test
    fun `empty history is a success with an empty list, not a failure`() =
        runTest {
            val repo: ValuationRepository = ValuationRepositoryImpl(FakeHistoryValuationApi(emptyList()))

            val result = repo.listHistory("h1")

            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()!!.isEmpty())
        }
}
