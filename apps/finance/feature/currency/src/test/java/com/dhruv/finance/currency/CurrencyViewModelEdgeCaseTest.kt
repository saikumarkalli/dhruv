package com.dhruv.finance.currency

import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.core.observability.NoOpPerformanceTracer
import com.dhruv.finance.data.CurrencyRateEntity
import com.dhruv.finance.data.ICurrencyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CurrencyViewModelEdgeCaseTest {
    private val dispatcher = StandardTestDispatcher()

    private class FakeRepo(
        private val rates: Map<String, Double> = emptyMap(),
        private val shouldFail: Boolean = false,
        private val cachedRates: List<CurrencyRateEntity> = emptyList(),
        private val timestamp: Long? = null,
    ) : ICurrencyRepository {
        override suspend fun getAllRates(): List<CurrencyRateEntity> =
            if (cachedRates.isNotEmpty()) {
                cachedRates
            } else {
                rates.map { (code, rate) -> CurrencyRateEntity(currencyCode = code, rate = rate, timestamp = timestamp ?: 0L) }
            }

        override suspend fun getRate(code: String): Double? = rates[code]

        override suspend fun getLastUpdateTimestamp(): Long? = timestamp

        override suspend fun fetchAndCacheLatestRates(baseCurrency: String): Result<Map<String, Double>> =
            if (shouldFail) {
                Result.failure(RuntimeException("Network error"))
            } else {
                Result.success(rates)
            }
    }

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm(repo: ICurrencyRepository) = CurrencyViewModel(repo, NoOpCrashReporter, NoOpPerformanceTracer)

    // ── Same-currency identity ──
    @Test
    fun sameCurrencyIdentity() =
        runTest(dispatcher) {
            val vm = vm(FakeRepo(mapOf("USD" to 1.0, "EUR" to 0.85)))
            advanceUntilIdle()
            vm.setCurrencyFrom("USD")
            vm.setCurrencyTo("USD")
            vm.setCurrencyInput("100")
            assertEquals("100", vm.currencyResult.value)
        }

    // ── Zero input ──
    @Test
    fun zeroInput() =
        runTest(dispatcher) {
            val vm = vm(FakeRepo(mapOf("USD" to 1.0, "EUR" to 0.85)))
            advanceUntilIdle()
            vm.setCurrencyFrom("USD")
            vm.setCurrencyTo("EUR")
            vm.setCurrencyInput("0")
            assertEquals("0", vm.currencyResult.value)
        }

    // ── Large amount ──
    @Test
    fun largeAmount() =
        runTest(dispatcher) {
            val vm = vm(FakeRepo(mapOf("USD" to 1.0, "INR" to 83.0)))
            advanceUntilIdle()
            vm.setCurrencyFrom("USD")
            vm.setCurrencyTo("INR")
            vm.setCurrencyInput("1000000")
            val result =
                vm.currencyResult.value
                    .replace(",", "")
                    .toDoubleOrNull()
            assertTrue("Large conversion should produce a valid number", result != null && result > 0)
        }

    // ── Empty rate map + network failure = Error ──
    @Test
    fun emptyRatesNetworkFailureGivesError() =
        runTest(dispatcher) {
            val vm = vm(FakeRepo(shouldFail = true))
            advanceUntilIdle()
            assertTrue(vm.currencyStatus.value is CurrencyViewModel.CurrencyStatus.Error)
        }

    // ── Network failure with cached rates = offline success ──
    @Test
    fun networkFailureWithCacheIsOfflineSuccess() =
        runTest(dispatcher) {
            val cached =
                listOf(
                    CurrencyRateEntity("USD", 1.0, System.currentTimeMillis()),
                    CurrencyRateEntity("EUR", 0.85, System.currentTimeMillis()),
                )
            val vm = vm(FakeRepo(shouldFail = true, cachedRates = cached))
            advanceUntilIdle()
            val status = vm.currencyStatus.value
            assertTrue("Should be offline success", status is CurrencyViewModel.CurrencyStatus.Success && status.isOffline)
        }

    // ── Empty string input ──
    @Test
    fun emptyStringInput() =
        runTest(dispatcher) {
            val vm = vm(FakeRepo(mapOf("USD" to 1.0, "EUR" to 0.85)))
            advanceUntilIdle()
            vm.setCurrencyInput("")
            assertEquals("", vm.currencyResult.value)
        }

    // ── Decimal input precision ──
    @Test
    fun decimalInput() =
        runTest(dispatcher) {
            val vm = vm(FakeRepo(mapOf("USD" to 1.0, "EUR" to 0.5)))
            advanceUntilIdle()
            vm.setCurrencyFrom("USD")
            vm.setCurrencyTo("EUR")
            vm.setCurrencyInput("0.5")
            assertEquals("0.25", vm.currencyResult.value)
        }

    // ── Cross-rate precision ──
    @Test
    fun crossRatePrecision() =
        runTest(dispatcher) {
            val vm = vm(FakeRepo(mapOf("USD" to 1.0, "EUR" to 0.85, "INR" to 83.12)))
            advanceUntilIdle()
            vm.setCurrencyFrom("EUR")
            vm.setCurrencyTo("INR")
            vm.setCurrencyInput("1")
            val result = vm.currencyResult.value.toDoubleOrNull()
            assertTrue("Cross-rate should be approximately 97.8", result != null && result > 97.0 && result < 99.0)
        }

    // ── Status starts as loading ──
    @Test
    fun initialStatusIsLoading() =
        runTest(dispatcher) {
            val vm = vm(FakeRepo(mapOf("USD" to 1.0)))
            assertEquals(CurrencyViewModel.CurrencyStatus.Loading, vm.currencyStatus.value)
        }
}
