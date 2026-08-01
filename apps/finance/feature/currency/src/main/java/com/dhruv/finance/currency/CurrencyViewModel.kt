package com.dhruv.finance.currency

import androidx.lifecycle.viewModelScope
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.FeatureViewModel
import com.dhruv.core.observability.PerformanceTracer
import com.dhruv.finance.data.ICurrencyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class CurrencyViewModel(
    private val currencyRepository: ICurrencyRepository,
    crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : FeatureViewModel(crashReporter, "currency") {
    // --- Currency Conversion State ---
    private val _currencyInput = MutableStateFlow("1")
    val currencyInput = _currencyInput.asStateFlow()

    private val _currencyFrom = MutableStateFlow("USD")
    val currencyFrom = _currencyFrom.asStateFlow()

    private val _currencyTo = MutableStateFlow("EUR")
    val currencyTo = _currencyTo.asStateFlow()

    private val _currencyResult = MutableStateFlow("")
    val currencyResult = _currencyResult.asStateFlow()

    private val _ratesMap = MutableStateFlow<Map<String, Double>>(emptyMap())
    val ratesMap = _ratesMap.asStateFlow()

    private val _currencyStatus = MutableStateFlow<CurrencyStatus>(CurrencyStatus.Loading)
    val currencyStatus = _currencyStatus.asStateFlow()

    private val _lastUpdatedTime = MutableStateFlow<Long?>(null)
    val lastUpdatedTime = _lastUpdatedTime.asStateFlow()

    private val staleThresholdMs = 24 * 60 * 60 * 1000L

    private val _isStale = MutableStateFlow(false)
    val isStale = _isStale.asStateFlow()

    sealed interface CurrencyStatus {
        object Loading : CurrencyStatus

        data class Success(
            val isOffline: Boolean,
        ) : CurrencyStatus

        data class Error(
            val message: String,
        ) : CurrencyStatus
    }

    val availableCurrencies =
        listOf(
            "USD",
            "EUR",
            "GBP",
            "INR",
            "JPY",
            "CAD",
            "AUD",
            "CNY",
            "CHF",
            "NZD",
            "ZAR",
            "SGD",
            "BRL",
        )

    init {
        crashReporter.setModule("currency")
        syncCurrencyRates()
    }

    fun syncCurrencyRates() {
        performanceTracer.trace("currency_sync") {
            _currencyStatus.value = CurrencyStatus.Loading
        }
        viewModelScope.launch(exceptionHandler) {
            val result = currencyRepository.fetchAndCacheLatestRates("USD")
            result
                .onSuccess { rates ->
                    _ratesMap.value = rates
                    val cached = currencyRepository.getAllRates()
                    val ts = cached.firstOrNull()?.timestamp ?: System.currentTimeMillis()
                    _lastUpdatedTime.value = ts
                    _isStale.value = false
                    _currencyStatus.value = CurrencyStatus.Success(isOffline = false)
                    recalculateCurrency()
                }.onFailure { err ->
                    crashReporter.recordException(err)
                    val cached = currencyRepository.getAllRates()
                    if (cached.isNotEmpty()) {
                        val map = cached.associate { it.currencyCode to it.rate }
                        _ratesMap.value = map
                        val ts = cached.firstOrNull()?.timestamp
                        _lastUpdatedTime.value = ts
                        _isStale.value = ts != null && (System.currentTimeMillis() - ts) > staleThresholdMs
                        _currencyStatus.value = CurrencyStatus.Success(isOffline = true)
                        recalculateCurrency()
                    } else {
                        _currencyStatus.value = CurrencyStatus.Error("No cached or live rates found.")
                    }
                }
        }
    }

    fun setCurrencyInput(input: String) {
        _currencyInput.value = input
        recalculateCurrency()
    }

    fun setCurrencyFrom(code: String) {
        _currencyFrom.value = code
        recalculateCurrency()
    }

    fun setCurrencyTo(code: String) {
        _currencyTo.value = code
        recalculateCurrency()
    }

    private fun recalculateCurrency() {
        val currentInput = _currencyInput.value.toDoubleOrNull()
        val rates = _ratesMap.value
        if (currentInput == null || rates.isEmpty()) {
            _currencyResult.value = ""
            return
        }
        val fromCode = _currencyFrom.value
        val toCode = _currencyTo.value
        val fromRate = rates[fromCode] ?: 1.0
        val toRate = rates[toCode] ?: 1.0

        // Convert via base USD
        val valueInUSD = currentInput / fromRate
        val finalVal = valueInUSD * toRate
        _currencyResult.value = formatValue(finalVal)
    }

    private fun formatValue(value: Double): String {
        val df = DecimalFormat("#.######", DecimalFormatSymbols(Locale.US))
        return df.format(value)
    }
}
