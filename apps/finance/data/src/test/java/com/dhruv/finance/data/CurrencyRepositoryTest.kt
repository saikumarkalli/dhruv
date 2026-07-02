package com.dhruv.finance.data

import com.dhruv.finance.data.api.CurrencyApiClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for [CurrencyRepository]'s self-healing fallback (PLATFORM.md §5 offline-first),
 * over a pure-JVM [FakeCurrencyRateDao]. The API client points at an unroutable host so both the
 * primary and fallback network calls fail fast, exercising the "fall back to local cache" branch
 * deterministically without a network, MockWebServer, or Robolectric.
 */
class CurrencyRepositoryTest {
    // Both URLs are unroutable → primary + fallback both throw → cache path is taken.
    private val offlineClient =
        CurrencyApiClient(
            primaryBaseUrl = "http://localhost:1/",
            fallbackBaseUrl = "http://localhost:1/",
            timeoutSeconds = 1,
            userAgent = "regression-test",
        )

    private val dao = FakeCurrencyRateDao()
    private val repo = CurrencyRepository(dao, offlineClient)

    @Test
    fun fetchFallsBackToCacheWhenAllApisFail() = runBlocking {
        dao.insertRates(listOf(CurrencyRateEntity("USD", 1.0, 0), CurrencyRateEntity("INR", 83.0, 0)))
        val result = repo.fetchAndCacheLatestRates("USD")
        assertTrue(result.isSuccess)
        assertEquals(83.0, result.getOrThrow()["INR"]!!, 0.0001)
    }

    @Test
    fun fetchFailsWhenApisFailAndCacheIsEmpty() = runBlocking {
        val result = repo.fetchAndCacheLatestRates("USD")
        assertTrue(result.isFailure)
    }

    @Test
    fun getRateReadsFromCacheAndNullsUnknown() = runBlocking {
        dao.insertRates(listOf(CurrencyRateEntity("EUR", 0.9, 0)))
        assertEquals(0.9, repo.getRate("EUR")!!, 0.0001)
        assertNull(repo.getRate("XYZ"))
    }

    @Test
    fun getAllRatesReturnsCachedEntities() = runBlocking {
        dao.insertRates(listOf(CurrencyRateEntity("USD", 1.0, 0)))
        assertEquals(1, repo.getAllRates().size)
    }
}
