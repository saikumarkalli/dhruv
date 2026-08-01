package com.dhruv.finance.unit

import com.dhruv.finance.data.util.CurrencyFormatter
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

/**
 * Edge-case tests for [UnitConverter] (this module) and [CurrencyFormatter] (:apps:finance:data).
 * Relocated from the app module during the Phase 4 feature split.
 */
class ConverterAndFormatterTest {
    private val converter: IUnitConverter = UnitConverter
    private val D = 1e-9

    // ── LENGTH ──
    @Test fun lengthMetersToMeters() = assertLength(5.0, LengthUnit.METERS, LengthUnit.METERS, 5.0)

    @Test fun lengthKmToKm() = assertLength(1.0, LengthUnit.KILOMETERS, LengthUnit.KILOMETERS, 1.0)

    @Test fun lengthMilesToMiles() = assertLength(3.0, LengthUnit.MILES, LengthUnit.MILES, 3.0)

    @Test fun lengthKmToMeters() = assertLength(2.0, LengthUnit.KILOMETERS, LengthUnit.METERS, 2000.0)

    @Test fun lengthMeterToKm() = assertLength(1000.0, LengthUnit.METERS, LengthUnit.KILOMETERS, 1.0)

    @Test fun lengthMeterToFeet() = assertLength(1.0, LengthUnit.METERS, LengthUnit.FEET, 1.0 / 0.3048)

    @Test fun lengthFeetToMeter() = assertLength(3.048, LengthUnit.FEET, LengthUnit.METERS, 3.048 * 0.3048)

    @Test fun lengthMeterToInches() = assertLength(1.0, LengthUnit.METERS, LengthUnit.INCHES, 1.0 / 0.0254)

    @Test fun lengthInchesToCm() = assertLength(1.0, LengthUnit.INCHES, LengthUnit.CENTIMETERS, 2.54)

    @Test fun lengthMilesToKm() = assertLength(1.0, LengthUnit.MILES, LengthUnit.KILOMETERS, 1.609344)

    @Test fun lengthKmToMiles() = assertLength(1.609344, LengthUnit.KILOMETERS, LengthUnit.MILES, 1.0)

    @Test fun lengthMmToMeter() = assertLength(1000.0, LengthUnit.MILLIMETERS, LengthUnit.METERS, 1.0)

    @Test fun lengthMeterToYards() = assertLength(0.9144, LengthUnit.METERS, LengthUnit.YARDS, 1.0)

    @Test fun lengthZeroValue() = assertLength(0.0, LengthUnit.KILOMETERS, LengthUnit.METERS, 0.0)

    @Test fun lengthNegative() = assertLength(-1.0, LengthUnit.KILOMETERS, LengthUnit.METERS, -1000.0)

    @Test fun lengthVeryLarge() = assertLength(1_000_000.0, LengthUnit.METERS, LengthUnit.KILOMETERS, 1000.0)

    // ── MASS ──
    @Test fun massKgToKg() = assertMass(5.0, MassUnit.KILOGRAMS, MassUnit.KILOGRAMS, 5.0)

    @Test fun massGramToGram() = assertMass(100.0, MassUnit.GRAMS, MassUnit.GRAMS, 100.0)

    @Test fun massKgToGrams() = assertMass(2.0, MassUnit.KILOGRAMS, MassUnit.GRAMS, 2000.0)

    @Test fun massGramToKg() = assertMass(1000.0, MassUnit.GRAMS, MassUnit.KILOGRAMS, 1.0)

    @Test fun massKgToPounds() = assertMass(1.0, MassUnit.KILOGRAMS, MassUnit.POUNDS, 1.0 / 0.45359237)

    @Test fun massPoundsToKg() = assertMass(1.0, MassUnit.POUNDS, MassUnit.KILOGRAMS, 0.45359237)

    @Test fun massKgToOunces() = assertMass(1.0, MassUnit.KILOGRAMS, MassUnit.OUNCES, 1.0 / 0.028349523125)

    @Test fun massMgToKg() = assertMass(1_000_000.0, MassUnit.MILLIGRAMS, MassUnit.KILOGRAMS, 1.0)

    @Test fun massMgToPounds() = assertMass(1_000_000.0, MassUnit.MILLIGRAMS, MassUnit.POUNDS, 1.0 / 0.45359237)

    @Test fun massOuncesToGrams() = assertMass(1.0, MassUnit.OUNCES, MassUnit.GRAMS, 0.028349523125 / 0.001)

    @Test fun massZero() = assertMass(0.0, MassUnit.KILOGRAMS, MassUnit.POUNDS, 0.0)

    @Test fun massNegative() = assertMass(-2.0, MassUnit.KILOGRAMS, MassUnit.GRAMS, -2000.0)

    @Test fun massVeryLarge() = assertMass(1_000_000.0, MassUnit.KILOGRAMS, MassUnit.GRAMS, 1e9)

    // ── TEMPERATURE ──
    // Affine (offset) conversions — NOT a ratio table. Pivoted through Celsius.
    @Test fun tempCelsiusToFahrenheitFreezing() = assertTemp(0.0, TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT, 32.0)

    @Test fun tempCelsiusToKelvinFreezing() = assertTemp(0.0, TemperatureUnit.CELSIUS, TemperatureUnit.KELVIN, 273.15)

    @Test fun tempCelsiusToFahrenheitBoiling() = assertTemp(100.0, TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT, 212.0)

    @Test fun tempCelsiusToKelvinBoiling() = assertTemp(100.0, TemperatureUnit.CELSIUS, TemperatureUnit.KELVIN, 373.15)

    @Test fun tempCelsiusToFahrenheitCrossover() = assertTemp(-40.0, TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT, -40.0)

    @Test fun tempFahrenheitToCelsiusCrossover() = assertTemp(-40.0, TemperatureUnit.FAHRENHEIT, TemperatureUnit.CELSIUS, -40.0)

    @Test fun tempFahrenheitToCelsiusNegative() = assertTemp(-4.0, TemperatureUnit.FAHRENHEIT, TemperatureUnit.CELSIUS, -20.0)

    @Test fun tempKelvinToCelsius() = assertTemp(300.0, TemperatureUnit.KELVIN, TemperatureUnit.CELSIUS, 26.85)

    @Test fun tempKelvinToFahrenheit() = assertTemp(0.0, TemperatureUnit.KELVIN, TemperatureUnit.FAHRENHEIT, -459.67)

    @Test fun tempFahrenheitToKelvin() = assertTemp(32.0, TemperatureUnit.FAHRENHEIT, TemperatureUnit.KELVIN, 273.15)

    @Test fun tempCelsiusSameUnitNoOp() = assertTemp(25.0, TemperatureUnit.CELSIUS, TemperatureUnit.CELSIUS, 25.0, 0.0)

    @Test fun tempFahrenheitSameUnitNoOp() = assertTemp(98.6, TemperatureUnit.FAHRENHEIT, TemperatureUnit.FAHRENHEIT, 98.6, 0.0)

    @Test fun tempKelvinSameUnitNoOp() = assertTemp(273.15, TemperatureUnit.KELVIN, TemperatureUnit.KELVIN, 273.15, 0.0)

    // Absolute-zero floor: NOT clamped (see class doc / task report) — matches lengthNegative /
    // massNegative above, which also don't reject physically-nonsensical negative values. -300°C
    // is below absolute zero (-273.15°C); this returns the mathematically "wrong" negative Kelvin
    // value rather than clamping to 0K, for consistency with the rest of this converter.
    @Test fun tempBelowAbsoluteZeroNotClamped() = assertTemp(-300.0, TemperatureUnit.CELSIUS, TemperatureUnit.KELVIN, -26.85)

    // ── AREA ──
    @Test fun areaSqmToSqm() = assertArea(5.0, AreaUnit.SQUARE_METERS, AreaUnit.SQUARE_METERS, 5.0)

    @Test fun areaSqkmToSqkm() = assertArea(2.0, AreaUnit.SQUARE_KILOMETERS, AreaUnit.SQUARE_KILOMETERS, 2.0)

    @Test fun areaSqmToSqft() = assertArea(1.0, AreaUnit.SQUARE_METERS, AreaUnit.SQUARE_FEET, 1.0 / 0.09290304)

    @Test fun areaSqftToSqm() = assertArea(1.0, AreaUnit.SQUARE_FEET, AreaUnit.SQUARE_METERS, 0.09290304)

    @Test fun areaHectareToSqm() = assertArea(1.0, AreaUnit.HECTARES, AreaUnit.SQUARE_METERS, 10_000.0)

    @Test fun areaSqmToHectare() = assertArea(20_000.0, AreaUnit.SQUARE_METERS, AreaUnit.HECTARES, 2.0)

    @Test fun areaAcreToSqm() = assertArea(1.0, AreaUnit.ACRES, AreaUnit.SQUARE_METERS, 4046.8564224)

    @Test fun areaSqkmToHectare() = assertArea(1.0, AreaUnit.SQUARE_KILOMETERS, AreaUnit.HECTARES, 100.0)

    @Test fun areaZero() = assertArea(0.0, AreaUnit.SQUARE_KILOMETERS, AreaUnit.SQUARE_METERS, 0.0)

    @Test fun areaNegative() = assertArea(-1.0, AreaUnit.HECTARES, AreaUnit.SQUARE_METERS, -10_000.0)

    @Test fun areaVeryLarge() = assertArea(1_000_000.0, AreaUnit.SQUARE_METERS, AreaUnit.SQUARE_KILOMETERS, 1.0)

    // ── MULTI-UNIT HELPER (backs Task 5's "12.5 km is also …" list) ──
    @Test
    fun multiUnitLengthExcludesSourceUnitAndConvertsAllOthers() {
        val results = UnitCategory.Length.convertToOtherUnits(1000.0, LengthUnit.METERS)
        val map = results.toMap()
        assertEquals(LengthUnit.entries.size - 1, results.size)
        assertFalse(map.containsKey(LengthUnit.METERS))
        assertEquals(1.0, map.getValue(LengthUnit.KILOMETERS), D)
        assertEquals(1000.0 / 0.3048, map.getValue(LengthUnit.FEET), 1e-6)
    }

    @Test
    fun multiUnitMassExcludesSourceUnitAndConvertsAllOthers() {
        val results = UnitCategory.Mass.convertToOtherUnits(1.0, MassUnit.KILOGRAMS)
        val map = results.toMap()
        assertEquals(MassUnit.entries.size - 1, results.size)
        assertFalse(map.containsKey(MassUnit.KILOGRAMS))
        assertEquals(1000.0, map.getValue(MassUnit.GRAMS), D)
        assertEquals(1.0 / 0.45359237, map.getValue(MassUnit.POUNDS), 1e-6)
    }

    @Test
    fun multiUnitAreaExcludesSourceUnitAndConvertsAllOthers() {
        val results = UnitCategory.Area.convertToOtherUnits(10_000.0, AreaUnit.SQUARE_METERS)
        val map = results.toMap()
        assertEquals(AreaUnit.entries.size - 1, results.size)
        assertFalse(map.containsKey(AreaUnit.SQUARE_METERS))
        assertEquals(1.0, map.getValue(AreaUnit.HECTARES), D)
    }

    @Test
    fun multiUnitTemperatureExcludesSourceUnitAndConvertsAllOthers() {
        val results = UnitCategory.Temperature.convertToOtherUnits(0.0, TemperatureUnit.CELSIUS)
        val map = results.toMap()
        assertEquals(TemperatureUnit.entries.size - 1, results.size)
        assertFalse(map.containsKey(TemperatureUnit.CELSIUS))
        assertEquals(32.0, map.getValue(TemperatureUnit.FAHRENHEIT), D)
        assertEquals(273.15, map.getValue(TemperatureUnit.KELVIN), D)
    }

    @Test
    fun multiUnitTemperatureSameUnitNoOpStillExcluded() {
        // Regression guard: filtering "every other unit" must compare by the from-unit's identity,
        // not by converted value — a same-valued coincidence elsewhere must not hide a real unit.
        val results = UnitCategory.Temperature.convertToOtherUnits(-40.0, TemperatureUnit.CELSIUS)
        assertEquals(2, results.size)
        assertTrue(results.any { it.first == TemperatureUnit.FAHRENHEIT })
        assertTrue(results.any { it.first == TemperatureUnit.KELVIN })
    }

    // ── CURRENCY FORMATTER ──
    @Test
    fun formatWholeNumber() {
        val result = CurrencyFormatter.format(BigDecimal("1000"), "₹")
        assertTrue("Expected 1,000.00 in '$result'", result.contains("1,000.00"))
    }

    @Test
    fun formatLargeIndianNumber() {
        val result = CurrencyFormatter.format(BigDecimal("100000"), "₹")
        assertTrue("Expected formatted number in '$result'", result.contains("100,000.00") || result.contains("1,00,000.00"))
    }

    @Test
    fun formatZero() {
        val result = CurrencyFormatter.format(BigDecimal.ZERO, "₹")
        assertTrue("Expected 0.00 in '$result'", result.contains("0.00"))
    }

    @Test
    fun formatDecimalRoundingHalfUp() {
        val result = CurrencyFormatter.format(BigDecimal("1.005"), "₹")
        assertTrue("Expected 1.01 in '$result'", result.contains("1.01"))
    }

    @Test
    fun formatDoubleNaN() {
        val result = CurrencyFormatter.format(Double.NaN)
        assertTrue("NaN should fallback to 0.00", result.contains("0.00"))
    }

    @Test
    fun formatDoubleInfinity() {
        val result = CurrencyFormatter.format(Double.POSITIVE_INFINITY)
        assertTrue("Infinity should fallback to 0.00", result.contains("0.00"))
    }

    @Test
    fun formatDoubleNegInfinity() {
        val result = CurrencyFormatter.format(Double.NEGATIVE_INFINITY)
        assertTrue("Negative Infinity should fallback to 0.00", result.contains("0.00"))
    }

    @Test
    fun formatCustomSymbol() {
        val result = CurrencyFormatter.format(BigDecimal("500"), "$")
        assertTrue("Should contain custom symbol '$'", result.startsWith("$"))
    }

    @Test
    fun formatSymbolWithTrailingSpace() {
        val result = CurrencyFormatter.format(BigDecimal("100"), "USD ")
        assertTrue("Trailing-space symbol should be used directly", result.startsWith("USD "))
        assertFalse("Should not have double space", result.startsWith("USD  "))
    }

    @Test
    fun formatDoubleValue() {
        val result = CurrencyFormatter.format(1234.56)
        assertTrue("Should contain 1,234.56", result.contains("1,234.56"))
    }

    @Test
    fun formatNegativeValue() {
        val result = CurrencyFormatter.format(BigDecimal("-500"), "₹")
        assertTrue("Should handle negative values", result.contains("500.00"))
    }

    private fun assertLength(
        value: Double,
        from: LengthUnit,
        to: LengthUnit,
        expected: Double,
        delta: Double = D,
    ) = assertEquals(expected, converter.convertLength(value, from, to), delta)

    private fun assertMass(
        value: Double,
        from: MassUnit,
        to: MassUnit,
        expected: Double,
        delta: Double = D,
    ) = assertEquals(expected, converter.convertMass(value, from, to), delta)

    private fun assertTemp(
        value: Double,
        from: TemperatureUnit,
        to: TemperatureUnit,
        expected: Double,
        delta: Double = D,
    ) = assertEquals(expected, converter.convertTemperature(value, from, to), delta)

    private fun assertArea(
        value: Double,
        from: AreaUnit,
        to: AreaUnit,
        expected: Double,
        delta: Double = D,
    ) = assertEquals(expected, converter.convertArea(value, from, to), delta)
}
