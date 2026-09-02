package com.dhruv.finance.data.tracker.repo

import com.dhruv.finance.data.tracker.dto.CreateLiabilityMetaRequestDto
import com.dhruv.finance.data.tracker.dto.LiabilityMetaDto
import com.dhruv.finance.data.tracker.dto.UpdateLiabilityMetaRequestDto
import com.dhruv.finance.data.tracker.model.CreateLiabilityMetaRequest
import com.dhruv.finance.data.tracker.model.LiabilityMeta
import com.dhruv.finance.data.tracker.model.LiabilityType
import com.dhruv.finance.data.tracker.model.UpdateLiabilityMetaRequest
import com.dhruv.finance.data.tracker.model.amortisationSplit
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

private class FakeLiabilityApi(
    private val rows: List<LiabilityMetaDto> = emptyList(),
    private val onInsert: (CreateLiabilityMetaRequestDto) -> List<LiabilityMetaDto> = { emptyList() },
    private val onUpdate: (String, UpdateLiabilityMetaRequestDto) -> List<LiabilityMetaDto> = { _, _ -> emptyList() },
) : LiabilityApi {
    var insertCallCount = 0
        private set
    var lastInsertBody: CreateLiabilityMetaRequestDto? = null
        private set
    var updateCallCount = 0
        private set
    var lastUpdateBody: UpdateLiabilityMetaRequestDto? = null
        private set

    override suspend fun listAll(notDeleted: String): List<LiabilityMetaDto> = rows

    override suspend fun getById(
        holdingIdFilter: String,
        notDeleted: String,
    ): List<LiabilityMetaDto> {
        val id = holdingIdFilter.removePrefix("eq.")
        return rows.filter { it.holdingId == id }
    }

    override suspend fun insert(body: CreateLiabilityMetaRequestDto): List<LiabilityMetaDto> {
        insertCallCount++
        lastInsertBody = body
        return onInsert(body)
    }

    override suspend fun update(
        holdingIdFilter: String,
        body: UpdateLiabilityMetaRequestDto,
    ): List<LiabilityMetaDto> {
        updateCallCount++
        lastUpdateBody = body
        return onUpdate(holdingIdFilter, body)
    }
}

class LiabilityRepositoryTest {
    // NW-BR-004-style: liability_type rejected if not in the fixed 4-value list, before any
    // network call.
    @Test
    fun `createMeta rejects an unknown liability type without calling the API`() =
        runTest {
            val api = FakeLiabilityApi()
            val repo: LiabilityRepository = LiabilityRepositoryImpl(api)

            val result =
                repo.createMeta(
                    CreateLiabilityMetaRequest(
                        holdingId = "h1",
                        liabilityTypeCode = "PERSONAL_LOAN",
                        rateBps = 900,
                    ),
                )

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalArgumentException)
            assertEquals(0, api.insertCallCount)
        }

    @Test
    fun `createMeta inserts a row with the validated liability type`() =
        runTest {
            val api =
                FakeLiabilityApi(
                    onInsert = {
                        listOf(LiabilityMetaDto(holdingId = "h1", liabilityType = "HOME_LOAN", rateBps = 850))
                    },
                )
            val repo: LiabilityRepository = LiabilityRepositoryImpl(api)

            val result =
                repo.createMeta(
                    CreateLiabilityMetaRequest(
                        holdingId = "h1",
                        liabilityTypeCode = "HOME_LOAN",
                        rateBps = 850,
                        emiPaise = 45_000_00L,
                        tenureMonths = 240,
                        originalPrincipalPaise = 50_00_000_00L,
                    ),
                )

            assertTrue(result.isSuccess)
            assertEquals(1, api.insertCallCount)
            assertEquals("HOME_LOAN", api.lastInsertBody?.liabilityType)
            assertEquals(850, api.lastInsertBody?.rateBps)
        }

    @Test
    fun `createMeta surfaces an API failure as Result failure`() =
        runTest {
            val api = FakeLiabilityApi(onInsert = { throw IOException("network down") })
            val repo: LiabilityRepository = LiabilityRepositoryImpl(api)

            val result =
                repo.createMeta(
                    CreateLiabilityMetaRequest(holdingId = "h1", liabilityTypeCode = "CREDIT_CARD", rateBps = 3600),
                )

            assertTrue(result.isFailure)
            assertEquals(1, api.insertCallCount)
        }

    @Test
    fun `get returns the mapped liability meta when it exists`() =
        runTest {
            val dto = LiabilityMetaDto(holdingId = "h1", liabilityType = "CAR_LOAN", rateBps = 950, paidMonths = 12)
            val repo: LiabilityRepository = LiabilityRepositoryImpl(FakeLiabilityApi(rows = listOf(dto)))

            val result = repo.get("h1")

            assertTrue(result.isSuccess)
            assertEquals(LiabilityType.CAR_LOAN, result.getOrNull()?.liabilityType)
            assertEquals(12, result.getOrNull()?.paidMonths)
        }

    @Test
    fun `get fails with NoSuchElementException when no meta row exists for the holding`() =
        runTest {
            val repo: LiabilityRepository = LiabilityRepositoryImpl(FakeLiabilityApi())

            val result = repo.get("missing")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NoSuchElementException)
        }

    @Test
    fun `listAll returns every mapped row`() =
        runTest {
            val rows =
                listOf(
                    LiabilityMetaDto(holdingId = "h1", liabilityType = "HOME_LOAN", rateBps = 850),
                    LiabilityMetaDto(holdingId = "h2", liabilityType = "CREDIT_CARD", rateBps = 3600),
                )
            val repo: LiabilityRepository = LiabilityRepositoryImpl(FakeLiabilityApi(rows = rows))

            val result = repo.listAll()

            assertTrue(result.isSuccess)
            assertEquals(2, result.getOrNull()?.size)
        }

    @Test
    fun `updateMeta issues exactly one PATCH with the full replacement body`() =
        runTest {
            val api = FakeLiabilityApi()
            val repo: LiabilityRepository = LiabilityRepositoryImpl(api)

            val result =
                repo.updateMeta(
                    holdingId = "h1",
                    request =
                        UpdateLiabilityMetaRequest(
                            rateBps = 900,
                            emiPaise = 46_000_00L,
                            debitDay = 5,
                            tenureMonths = 240,
                            paidMonths = 13,
                            originalPrincipalPaise = 50_00_000_00L,
                            collateral = "Flat, Bengaluru",
                        ),
                )

            assertTrue(result.isSuccess)
            assertEquals(1, api.updateCallCount)
            assertEquals(13, api.lastUpdateBody?.paidMonths)
            assertEquals(900, api.lastUpdateBody?.rateBps)
        }

    // spec.md Story 4 Scenario 2: principal paid + interest paid + remaining sums to the total
    // obligation (money paid so far + money still owed) — never to the original principal alone.
    @Test
    fun `amortisationSplit sums to the total obligation`() {
        val meta =
            LiabilityMeta(
                holdingId = "h1",
                liabilityType = LiabilityType.HOME_LOAN,
                rateBps = 850,
                emiPaise = 45_000_00L,
                debitDay = 5,
                tenureMonths = 240,
                paidMonths = 24,
                originalPrincipalPaise = 50_00_000_00L,
                collateral = null,
                linkedAccountId = null,
            )
        val remainingPaise = 47_50_000_00L

        val split = meta.amortisationSplit(remainingPaise)!!

        val totalPaidSoFar = meta.emiPaise!! * meta.paidMonths
        assertEquals(totalPaidSoFar + split.remainingPaise, split.principalPaidPaise + split.interestPaidPaise + split.remainingPaise)
        assertEquals(remainingPaise, split.remainingPaise)
        assertEquals(meta.originalPrincipalPaise!! - remainingPaise, split.principalPaidPaise)
    }

    @Test
    fun `amortisationSplit is null when the liability has no original principal (a card or BNPL line)`() {
        val meta =
            LiabilityMeta(
                holdingId = "h1",
                liabilityType = LiabilityType.CREDIT_CARD,
                rateBps = 3600,
                emiPaise = null,
                debitDay = 10,
                tenureMonths = null,
                paidMonths = 0,
                originalPrincipalPaise = null,
                collateral = null,
                linkedAccountId = null,
            )

        assertNull(meta.amortisationSplit(25_000_00L))
    }
}
