package com.dhruv.finance.unit

import androidx.lifecycle.ViewModel
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.PerformanceTracer
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class UnitViewModel(
    private val crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : ViewModel() {
    private val _featureError = MutableStateFlow<Throwable?>(null)
    val featureError: StateFlow<Throwable?> = _featureError.asStateFlow()

    @Suppress("unused") // consumed by FeatureHost in the app shell
    private val exceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            crashReporter.recordException(throwable)
            _featureError.value = throwable
        }

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

    init {
        crashReporter.setModule("unit")
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
        performanceTracer.trace("unit_convert") {
            val valDouble = _lengthInput.value.toDoubleOrNull()
            if (valDouble == null) {
                _lengthResult.value = ""
                return@trace
            }
            val converted = LengthUnit.convert(valDouble, _lengthFromUnit.value, _lengthToUnit.value)
            _lengthResult.value = formatValue(converted)
        }
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
        performanceTracer.trace("unit_convert") {
            val valDouble = _massInput.value.toDoubleOrNull()
            if (valDouble == null) {
                _massResult.value = ""
                return@trace
            }
            val converted = MassUnit.convert(valDouble, _massFromUnit.value, _massToUnit.value)
            _massResult.value = formatValue(converted)
        }
    }

    private fun formatValue(value: Double): String {
        val df = DecimalFormat("#.######", DecimalFormatSymbols(Locale.US))
        return df.format(value)
    }
}
