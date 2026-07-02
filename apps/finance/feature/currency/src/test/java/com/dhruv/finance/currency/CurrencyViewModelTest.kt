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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CurrencyViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val rates = mapOf("USD" to 1.0, "EUR" to 0.5, "INR" to 80.0)

    // Fake so the conversion math is tested without a real network/Room dependency.
    private class FakeCurrencyRepository(
        private val rates: Map<String, Double>,
    ) : ICurrencyRepository {
        override suspend fun getAllRates(): List<CurrencyRateEntity> =
            rates.map { (code, rate) -> CurrencyRateEntity(currencyCode = code, rate = rate, timestamp = 0L) }

        override suspend fun getRate(code: String): Double? = rates[code]

        override suspend fun fetchAndCacheLatestRates(baseCurrency: String): Result<Map<String, Double>> = Result.success(rates)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = CurrencyViewModel(FakeCurrencyRepository(rates), NoOpCrashReporter, NoOpPerformanceTracer)

    @Test
    fun convertsViaUsdBase() =
        runTest(dispatcher) {
            val vm = viewModel()
            advanceUntilIdle()
            vm.setCurrencyFrom("USD")
            vm.setCurrencyTo("INR")
            vm.setCurrencyInput("10")
            assertEquals("800", vm.currencyResult.value)
        }

    @Test
    fun convertsBetweenTwoNonUsdCurrencies() =
        runTest(dispatcher) {
            val vm = viewModel()
            advanceUntilIdle()
            vm.setCurrencyFrom("EUR")
            vm.setCurrencyTo("INR")
            vm.setCurrencyInput("10")
            // 10 EUR -> USD (10 / 0.5 = 20) -> INR (20 * 80 = 1600)
            assertEquals("1600", vm.currencyResult.value)
        }

    @Test
    fun statusBecomesSuccessAfterSync() =
        runTest(dispatcher) {
            val vm = viewModel()
            advanceUntilIdle()
            assertEquals(
                CurrencyViewModel.CurrencyStatus.Success(isOffline = false),
                vm.currencyStatus.value,
            )
        }

    @Test
    fun nonNumericInputClearsResult() =
        runTest(dispatcher) {
            val vm = viewModel()
            advanceUntilIdle()
            vm.setCurrencyInput("abc")
            assertEquals("", vm.currencyResult.value)
        }
}
