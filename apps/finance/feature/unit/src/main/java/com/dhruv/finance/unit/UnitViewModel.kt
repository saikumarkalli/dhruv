package com.dhruv.finance.unit

import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.FeatureViewModel
import com.dhruv.core.observability.PerformanceTracer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class UnitViewModel(
    crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : FeatureViewModel(crashReporter, "unit") {

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

    // --- Area Conversion State ---
    private val _areaInput = MutableStateFlow("1")
    val areaInput = _areaInput.asStateFlow()

    private val _areaFromUnit = MutableStateFlow(AreaUnit.SQUARE_METERS)
    val areaFromUnit = _areaFromUnit.asStateFlow()

    private val _areaToUnit = MutableStateFlow(AreaUnit.SQUARE_FEET)
    val areaToUnit = _areaToUnit.asStateFlow()

    private val _areaResult = MutableStateFlow("10.76391")
    val areaResult = _areaResult.asStateFlow()

    // --- Temperature Conversion State ---
    private val _tempInput = MutableStateFlow("1")
    val tempInput = _tempInput.asStateFlow()

    private val _tempFromUnit = MutableStateFlow(TemperatureUnit.CELSIUS)
    val tempFromUnit = _tempFromUnit.asStateFlow()

    private val _tempToUnit = MutableStateFlow(TemperatureUnit.FAHRENHEIT)
    val tempToUnit = _tempToUnit.asStateFlow()

    private val _tempResult = MutableStateFlow("33.8")
    val tempResult = _tempResult.asStateFlow()

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

    // --- Area Logic ---
    fun setAreaInput(input: String) {
        _areaInput.value = input
        recalculateArea()
    }

    fun setAreaFromUnit(unit: AreaUnit) {
        _areaFromUnit.value = unit
        recalculateArea()
    }

    fun setAreaToUnit(unit: AreaUnit) {
        _areaToUnit.value = unit
        recalculateArea()
    }

    private fun recalculateArea() {
        performanceTracer.trace("unit_convert") {
            val valDouble = _areaInput.value.toDoubleOrNull()
            if (valDouble == null) {
                _areaResult.value = ""
                return@trace
            }
            val converted = AreaUnit.convert(valDouble, _areaFromUnit.value, _areaToUnit.value)
            _areaResult.value = formatValue(converted)
        }
    }

    // --- Temperature Logic ---
    fun setTempInput(input: String) {
        _tempInput.value = input
        recalculateTemp()
    }

    fun setTempFromUnit(unit: TemperatureUnit) {
        _tempFromUnit.value = unit
        recalculateTemp()
    }

    fun setTempToUnit(unit: TemperatureUnit) {
        _tempToUnit.value = unit
        recalculateTemp()
    }

    private fun recalculateTemp() {
        performanceTracer.trace("unit_convert") {
            val valDouble = _tempInput.value.toDoubleOrNull()
            if (valDouble == null) {
                _tempResult.value = ""
                return@trace
            }
            val converted = TemperatureUnit.convert(valDouble, _tempFromUnit.value, _tempToUnit.value)
            _tempResult.value = formatValue(converted)
        }
    }

    private fun formatValue(value: Double): String {
        val df = DecimalFormat("#.######", DecimalFormatSymbols(Locale.US))
        return df.format(value)
    }
}
