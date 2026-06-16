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
    @Test fun lengthMetersToMeters()       = assertLength(5.0, LengthUnit.METERS, LengthUnit.METERS, 5.0)
    @Test fun lengthKmToKm()               = assertLength(1.0, LengthUnit.KILOMETERS, LengthUnit.KILOMETERS, 1.0)
    @Test fun lengthMilesToMiles()         = assertLength(3.0, LengthUnit.MILES, LengthUnit.MILES, 3.0)
    @Test fun lengthKmToMeters()           = assertLength(2.0, LengthUnit.KILOMETERS, LengthUnit.METERS, 2000.0)
    @Test fun lengthMeterToKm()            = assertLength(1000.0, LengthUnit.METERS, LengthUnit.KILOMETERS, 1.0)
    @Test fun lengthMeterToFeet()          = assertLength(1.0, LengthUnit.METERS, LengthUnit.FEET, 1.0 / 0.3048)
    @Test fun lengthFeetToMeter()          = assertLength(3.048, LengthUnit.FEET, LengthUnit.METERS, 3.048 * 0.3048)
    @Test fun lengthMeterToInches()        = assertLength(1.0, LengthUnit.METERS, LengthUnit.INCHES, 1.0 / 0.0254)
    @Test fun lengthInchesToCm()           = assertLength(1.0, LengthUnit.INCHES, LengthUnit.CENTIMETERS, 2.54)
    @Test fun lengthMilesToKm()            = assertLength(1.0, LengthUnit.MILES, LengthUnit.KILOMETERS, 1.609344)
    @Test fun lengthKmToMiles()            = assertLength(1.609344, LengthUnit.KILOMETERS, LengthUnit.MILES, 1.0)
    @Test fun lengthMmToMeter()            = assertLength(1000.0, LengthUnit.MILLIMETERS, LengthUnit.METERS, 1.0)
    @Test fun lengthMeterToYards()         = assertLength(0.9144, LengthUnit.METERS, LengthUnit.YARDS, 1.0)
    @Test fun lengthZeroValue()            = assertLength(0.0, LengthUnit.KILOMETERS, LengthUnit.METERS, 0.0)
    @Test fun lengthNegative()             = assertLength(-1.0, LengthUnit.KILOMETERS, LengthUnit.METERS, -1000.0)
    @Test fun lengthVeryLarge()            = assertLength(1_000_000.0, LengthUnit.METERS, LengthUnit.KILOMETERS, 1000.0)

    // ── MASS ──
    @Test fun massKgToKg()         = assertMass(5.0, MassUnit.KILOGRAMS, MassUnit.KILOGRAMS, 5.0)
    @Test fun massGramToGram()     = assertMass(100.0, MassUnit.GRAMS, MassUnit.GRAMS, 100.0)
    @Test fun massKgToGrams()      = assertMass(2.0, MassUnit.KILOGRAMS, MassUnit.GRAMS, 2000.0)
    @Test fun massGramToKg()       = assertMass(1000.0, MassUnit.GRAMS, MassUnit.KILOGRAMS, 1.0)
    @Test fun massKgToPounds()     = assertMass(1.0, MassUnit.KILOGRAMS, MassUnit.POUNDS, 1.0 / 0.45359237)
    @Test fun massPoundsToKg()     = assertMass(1.0, MassUnit.POUNDS, MassUnit.KILOGRAMS, 0.45359237)
    @Test fun massKgToOunces()     = assertMass(1.0, MassUnit.KILOGRAMS, MassUnit.OUNCES, 1.0 / 0.028349523125)
    @Test fun massMgToKg()         = assertMass(1_000_000.0, MassUnit.MILLIGRAMS, MassUnit.KILOGRAMS, 1.0)
    @Test fun massMgToPounds()     = assertMass(1_000_000.0, MassUnit.MILLIGRAMS, MassUnit.POUNDS, 1.0 / 0.45359237)
    @Test fun massOuncesToGrams()  = assertMass(1.0, MassUnit.OUNCES, MassUnit.GRAMS, 0.028349523125 / 0.001)
    @Test fun massZero()           = assertMass(0.0, MassUnit.KILOGRAMS, MassUnit.POUNDS, 0.0)
    @Test fun massNegative()       = assertMass(-2.0, MassUnit.KILOGRAMS, MassUnit.GRAMS, -2000.0)
    @Test fun massVeryLarge()      = assertMass(1_000_000.0, MassUnit.KILOGRAMS, MassUnit.GRAMS, 1e9)

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

    private fun assertLength(value: Double, from: LengthUnit, to: LengthUnit, expected: Double, delta: Double = D) =
        assertEquals(expected, converter.convertLength(value, from, to), delta)

    private fun assertMass(value: Double, from: MassUnit, to: MassUnit, expected: Double, delta: Double = D) =
        assertEquals(expected, converter.convertMass(value, from, to), delta)
}
