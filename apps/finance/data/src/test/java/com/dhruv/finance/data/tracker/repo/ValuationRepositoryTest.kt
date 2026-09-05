package com.dhruv.finance.data.tracker.repo

import com.dhruv.finance.data.tracker.dto.CorrectValuationRequestDto
import com.dhruv.finance.data.tracker.dto.LatestValuationRowDto
import com.dhruv.finance.data.tracker.dto.RecordValuationRequestDto
import com.dhruv.finance.data.tracker.dto.ValuationDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeHistoryValuationApi(
    private val rows: List<ValuationDto> = emptyList(),
    private val onInsert: (RecordValuationRequestDto) -> List<ValuationDto> = { emptyList() },
    private val onCorrect: (CorrectValuationRequestDto) -> String = { "corrected-id" },
) : ValuationApi {
    var insertCallCount = 0
        private set
    var lastInsertBody: RecordValuationRequestDto? = null
        private set
    var correctCallCount = 0
        private set
    var lastCorrectBody: CorrectValuationRequestDto? = null
        private set

    override suspend fun listLatestValuations(): List<LatestValuationRowDto> =
        throw UnsupportedOperationException("not exercised by this test")

    override suspend fun listHistory(
        holdingIdFilter: String,
        notDeleted: String,
        order: String,
    ): List<ValuationDto> = rows

    override suspend fun insertValuation(body: RecordValuationRequestDto): List<ValuationDto> {
        insertCallCount++
        lastInsertBody = body
        return onInsert(body)
    }

    override suspend fun correctValuation(body: CorrectValuationRequestDto): String {
        correctCallCount++
        lastCorrectBody = body
        return onCorrect(body)
    }
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
            val repo: ValuationRepository = ValuationRepositoryImpl(FakeHistoryValuationApi(rows = rows))

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
            val repo: ValuationRepository = ValuationRepositoryImpl(FakeHistoryValuationApi(rows = rows))

            val entries = repo.listHistory("h1").getOrThrow()

            assertEquals(1, entries.size)
            assertNull(entries[0].deltaPaise)
        }

    @Test
    fun `empty history is a success with an empty list, not a failure`() =
        runTest {
            val repo: ValuationRepository = ValuationRepositoryImpl(FakeHistoryValuationApi())

            val result = repo.listHistory("h1")

            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()!!.isEmpty())
        }

    // NW-BR-001-adjacent: recordValue is a single plain insert — a genuinely new value, not an
    // amendment of any existing row.
    @Test
    fun `recordValue inserts a new row with the requested source`() =
        runTest {
            val api =
                FakeHistoryValuationApi(
                    onInsert = { listOf(ValuationDto("new-id", "h1", 6_00_000_00L, "2026-08-15", "MANUAL")) },
                )
            val repo: ValuationRepository = ValuationRepositoryImpl(api)

            val result = repo.recordValue(holdingId = "h1", valuePaise = 6_00_000_00L, asOf = "2026-08-15")

            assertTrue(result.isSuccess)
            assertEquals("new-id", result.getOrNull())
            assertEquals(1, api.insertCallCount)
            assertEquals(0, api.correctCallCount)
            assertEquals("h1", api.lastInsertBody?.holdingId)
            assertEquals("MANUAL", api.lastInsertBody?.source)
        }

    @Test
    fun `recordValue rejects CORRECTION as a source — that source is server-assigned only`() =
        runTest {
            val api = FakeHistoryValuationApi()
            val repo: ValuationRepository = ValuationRepositoryImpl(api)

            val result = repo.recordValue(holdingId = "h1", valuePaise = 100L, asOf = "2026-08-15", sourceCode = "CORRECTION")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalArgumentException)
            assertEquals(0, api.insertCallCount)
        }

    @Test
    fun `recordValue rejects an unknown source code without calling the API`() =
        runTest {
            val api = FakeHistoryValuationApi()
            val repo: ValuationRepository = ValuationRepositoryImpl(api)

            val result = repo.recordValue(holdingId = "h1", valuePaise = 100L, asOf = "2026-08-15", sourceCode = "GUESS")

            assertTrue(result.isFailure)
            assertEquals(0, api.insertCallCount)
        }

    // NW-BR-002/NW-BR-003: a correction is exactly one RPC call — the soft-delete-old +
    // append-corrected transaction happens entirely server-side; the client never issues an
    // UPDATE against value_paise and never makes two separate calls for a correction.
    @Test
    fun `correctValue calls the RPC exactly once and returns the corrected row's id`() =
        runTest {
            val api = FakeHistoryValuationApi(onCorrect = { "corrected-new-id" })
            val repo: ValuationRepository = ValuationRepositoryImpl(api)

            val result = repo.correctValue(valuationId = "v-wrong", valuePaise = 4_75_000_00L, asOf = "2026-07-01")

            assertTrue(result.isSuccess)
            assertEquals("corrected-new-id", result.getOrNull())
            assertEquals(1, api.correctCallCount)
            assertEquals(0, api.insertCallCount)
            assertEquals("v-wrong", api.lastCorrectBody?.valuationId)
            assertEquals(4_75_000_00L, api.lastCorrectBody?.valuePaise)
        }

    @Test
    fun `correctValue surfaces an RPC failure as Result failure`() =
        runTest {
            val api = FakeHistoryValuationApi(onCorrect = { throw java.io.IOException("network down") })
            val repo: ValuationRepository = ValuationRepositoryImpl(api)

            val result = repo.correctValue(valuationId = "v-wrong", valuePaise = 100L, asOf = "2026-07-01")

            assertTrue(result.isFailure)
            assertEquals(1, api.correctCallCount)
        }
}
