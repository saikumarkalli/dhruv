package com.example.ui.converter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CurrencyRepository
import com.example.util.LengthUnit
import com.example.util.MassUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class ConverterViewModel(
    private val currencyRepository: CurrencyRepository
) : ViewModel() {

    // --- Length Conversion State ---
    private val _lengthInput = MutableStateFlow("1")
    val lengthInput = _lengthInput.asStateFlow()

    private val _lengthFromUnit = MutableStateFlow(LengthUnit.METERS)
    val lengthFromUnit = _lengthFromUnit.asStateFlow()

    private val _lengthToUnit = MutableStateFlow(LengthUnit.FEET)
    val lengthToUnit = _lengthToUnit.asStateFlow()

    private val _lengthResult = MutableStateFlow("3.28084")
    val lengthResult = _lengthResult.asStateFlow()


    // --- Mass Conversion State ---
    private val _massInput = MutableStateFlow("1")
    val massInput = _massInput.asStateFlow()

    private val _massFromUnit = MutableStateFlow(MassUnit.KILOGRAMS)
    val massFromUnit = _massFromUnit.asStateFlow()

    private val _massToUnit = MutableStateFlow(MassUnit.POUNDS)
    val massToUnit = _massToUnit.asStateFlow()

    private val _massResult = MutableStateFlow("2.20462")
    val massResult = _massResult.asStateFlow()


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

    sealed interface CurrencyStatus {
        object Loading : CurrencyStatus
        data class Success(val isOffline: Boolean) : CurrencyStatus
        data class Error(val message: String) : CurrencyStatus
    }

    val availableCurrencies = listOf(
        "USD", "EUR", "GBP", "INR", "JPY", "CAD", "AUD", "CNY", "CHF", "NZD", "ZAR", "SGD", "BRL"
    )

    init {
        syncCurrencyRates()
    }

    fun syncCurrencyRates() {
        _currencyStatus.value = CurrencyStatus.Loading
        viewModelScope.launch {
            val result = currencyRepository.fetchAndCacheLatestRates("USD")
            result.onSuccess { rates ->
                _ratesMap.value = rates
                val cached = currencyRepository.getAllRates()
                val ts = cached.firstOrNull()?.timestamp ?: System.currentTimeMillis()
                _lastUpdatedTime.value = ts
                _currencyStatus.value = CurrencyStatus.Success(isOffline = false)
                recalculateCurrency()
            }.onFailure { err ->
                val cached = currencyRepository.getAllRates()
                if (cached.isNotEmpty()) {
                    val map = cached.associate { it.currencyCode to it.rate }
                    _ratesMap.value = map
                    _lastUpdatedTime.value = cached.firstOrNull()?.timestamp
                    _currencyStatus.value = CurrencyStatus.Success(isOffline = true)
                    recalculateCurrency()
                } else {
                    _currencyStatus.value = CurrencyStatus.Error("No cached or live rates found.")
                }
            }
        }
    }

    // --- Length Logic ---
    fun setLengthInput(input: String) {
        _lengthInput.value = input
        recalculateLength()
    }

    fun setLengthFromUnit(unit: LengthUnit) {
        _lengthFromUnit.value = unit
        recalculateLength()
    }

    fun setLengthToUnit(unit: LengthUnit) {
        _lengthToUnit.value = unit
        recalculateLength()
    }

    private fun recalculateLength() {
        val valDouble = _lengthInput.value.toDoubleOrNull()
        if (valDouble == null) {
            _lengthResult.value = ""
            return
        }
        val converted = LengthUnit.convert(valDouble, _lengthFromUnit.value, _lengthToUnit.value)
        _lengthResult.value = formatValue(converted)
    }

    // --- Mass Logic ---
    fun setMassInput(input: String) {
        _massInput.value = input
        recalculateMass()
    }

    fun setMassFromUnit(unit: MassUnit) {
        _massFromUnit.value = unit
        recalculateMass()
    }

    fun setMassToUnit(unit: MassUnit) {
        _massToUnit.value = unit
        recalculateMass()
    }

    private fun recalculateMass() {
        val valDouble = _massInput.value.toDoubleOrNull()
        if (valDouble == null) {
            _massResult.value = ""
            return
        }
        val converted = MassUnit.convert(valDouble, _massFromUnit.value, _massToUnit.value)
        _massResult.value = formatValue(converted)
    }

    // --- Currency Logic ---
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

        // Convert base USD
        val valueInUSD = currentInput / fromRate
        val finalVal = valueInUSD * toRate
        _currencyResult.value = formatValue(finalVal)
    }

    private fun formatValue(value: Double): String {
        val df = DecimalFormat("#.######", DecimalFormatSymbols(Locale.US))
        return df.format(value)
    }

    class Factory(
        private val currencyRepository: CurrencyRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ConverterViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ConverterViewModel(currencyRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
