package com.dhruv.finance.data.tracker.repo

import com.dhruv.finance.data.tracker.dto.CreateHoldingWithValueRequestDto
import com.dhruv.finance.data.tracker.dto.HoldingDto
import com.dhruv.finance.data.tracker.dto.LatestValuationRowDto
import com.dhruv.finance.data.tracker.model.CreateHoldingRequest
import com.dhruv.finance.data.tracker.model.HoldingKind
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

private class FakeHoldingApi(
    private val onCreate: (CreateHoldingWithValueRequestDto) -> String = { "holding-id" },
    private val byId: Map<String, HoldingDto> = emptyMap(),
) : HoldingApi {
    var createCallCount = 0
        private set
    var lastRequest: CreateHoldingWithValueRequestDto? = null
        private set

    override suspend fun createHoldingWithValue(body: CreateHoldingWithValueRequestDto): String {
        createCallCount++
        lastRequest = body
        return onCreate(body)
    }

    override suspend fun listHoldings(
        kindFilter: String,
        notDeleted: String,
        order: String,
    ): List<HoldingDto> = throw UnsupportedOperationException("not exercised by this test")

    override suspend fun getById(
        idFilter: String,
        notDeleted: String,
    ): List<HoldingDto> {
        val id = idFilter.removePrefix("eq.")
        return listOfNotNull(byId[id])
    }
}

private class FakeValuationApi : ValuationApi {
    override suspend fun listLatestValuations(): List<LatestValuationRowDto> = emptyList()

    override suspend fun listHistory(
        holdingIdFilter: String,
        notDeleted: String,
        order: String,
    ): List<com.dhruv.finance.data.tracker.dto.ValuationDto> = throw UnsupportedOperationException("not exercised by this test")

    override suspend fun insertValuation(
        body: com.dhruv.finance.data.tracker.dto.RecordValuationRequestDto,
    ): List<com.dhruv.finance.data.tracker.dto.ValuationDto> = throw UnsupportedOperationException("not exercised by this test")

    override suspend fun correctValuation(
        body: com.dhruv.finance.data.tracker.dto.CorrectValuationRequestDto,
    ): String = throw UnsupportedOperationException("not exercised by this test")
}

class HoldingRepositoryTest {
    // NW-BR-004: sector rejected if not in the fixed 10-value list, before any network call.
    @Test
    fun `createWithFirstValuation rejects an unknown sector code without calling the API`() =
        runTest {
            val api = FakeHoldingApi()
            val repo: HoldingRepository = HoldingRepositoryImpl(api, FakeValuationApi())

            val result =
                repo.createWithFirstValuation(
                    CreateHoldingRequest(
                        name = "Test",
                        kind = HoldingKind.ASSET,
                        sectorCode = "CRYPTOCURRENCY",
                        valuePaise = 10_00L,
                        asOf = "2026-01-01",
                    ),
                )

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalArgumentException)
            assertEquals(0, api.createCallCount)
        }

    // NW-BR-001/BR-C2: holding + first valuation written atomically — a single API call, never two.
    @Test
    fun `createWithFirstValuation writes the holding and its first valuation in a single call`() =
        runTest {
            val api = FakeHoldingApi(onCreate = { "new-holding-id" })
            val repo: HoldingRepository = HoldingRepositoryImpl(api, FakeValuationApi())

            val result =
                repo.createWithFirstValuation(
                    CreateHoldingRequest(
                        name = "HDFC Savings",
                        kind = HoldingKind.ASSET,
                        sectorCode = "BANK",
                        valuePaise = 50_000_00L,
                        asOf = "2026-08-31",
                    ),
                )

            assertTrue(result.isSuccess)
            assertEquals("new-holding-id", result.getOrNull())
            assertEquals(1, api.createCallCount)
            val sentBody = api.lastRequest
            assertEquals("HDFC Savings", sentBody?.name)
            assertEquals("ASSET", sentBody?.kind)
            assertEquals("BANK", sentBody?.sector)
            assertEquals(50_000_00L, sentBody?.valuePaise)
        }

    @Test
    fun `createWithFirstValuation surfaces an API failure as Result failure`() =
        runTest {
            val api = FakeHoldingApi(onCreate = { throw IOException("network down") })
            val repo: HoldingRepository = HoldingRepositoryImpl(api, FakeValuationApi())

            val result =
                repo.createWithFirstValuation(
                    CreateHoldingRequest(
                        name = "Test",
                        kind = HoldingKind.ASSET,
                        sectorCode = "CASH",
                        valuePaise = 100L,
                        asOf = "2026-01-01",
                    ),
                )

            assertTrue(result.isFailure)
            assertEquals(1, api.createCallCount)
        }

    @Test
    fun `get returns the mapped holding when it exists`() =
        runTest {
            val dto = HoldingDto(id = "h1", name = "HDFC Savings", kind = "ASSET", sector = "BANK")
            val api = FakeHoldingApi(byId = mapOf("h1" to dto))
            val repo: HoldingRepository = HoldingRepositoryImpl(api, FakeValuationApi())

            val result = repo.get("h1")

            assertTrue(result.isSuccess)
            assertEquals("HDFC Savings", result.getOrNull()?.name)
        }

    @Test
    fun `get fails with NoSuchElementException when the holding doesn't exist`() =
        runTest {
            val api = FakeHoldingApi()
            val repo: HoldingRepository = HoldingRepositoryImpl(api, FakeValuationApi())

            val result = repo.get("missing")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NoSuchElementException)
        }
}
