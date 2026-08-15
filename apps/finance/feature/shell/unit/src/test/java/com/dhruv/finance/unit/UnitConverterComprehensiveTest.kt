package com.dhruv.finance.unit

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConverterComprehensiveTest {
    private val delta = 1e-6
    private val converter: IUnitConverter = UnitConverter

    // ── Temperature: C ↔ F ──

    @Test
    fun tempCToF_boiling() =
        assertEquals(
            212.0,
            converter.convertTemperature(100.0, TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT),
            delta,
        )

    @Test
    fun tempCToF_freezing() =
        assertEquals(
            32.0,
            converter.convertTemperature(0.0, TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT),
            delta,
        )

    @Test
    fun tempCToF_minus40() =
        assertEquals(
            -40.0,
            converter.convertTemperature(-40.0, TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT),
            delta,
        )

    @Test
    fun tempFToC_212() =
        assertEquals(
            100.0,
            converter.convertTemperature(212.0, TemperatureUnit.FAHRENHEIT, TemperatureUnit.CELSIUS),
            delta,
        )

    @Test
    fun tempFToC_32() =
        assertEquals(
            0.0,
            converter.convertTemperature(32.0, TemperatureUnit.FAHRENHEIT, TemperatureUnit.CELSIUS),
            delta,
        )

    @Test
    fun tempFToC_minus40() =
        assertEquals(
            -40.0,
            converter.convertTemperature(-40.0, TemperatureUnit.FAHRENHEIT, TemperatureUnit.CELSIUS),
            delta,
        )

    // ── Temperature: K ↔ C ──

    @Test
    fun tempKToC_absoluteZero() =
        assertEquals(
            -273.15,
            converter.convertTemperature(0.0, TemperatureUnit.KELVIN, TemperatureUnit.CELSIUS),
            delta,
        )

    @Test
    fun tempCToK_0() =
        assertEquals(
            273.15,
            converter.convertTemperature(0.0, TemperatureUnit.CELSIUS, TemperatureUnit.KELVIN),
            delta,
        )

    @Test
    fun tempKToC_373() =
        assertEquals(
            100.0,
            converter.convertTemperature(373.15, TemperatureUnit.KELVIN, TemperatureUnit.CELSIUS),
            delta,
        )

    @Test
    fun tempCToK_100() =
        assertEquals(
            373.15,
            converter.convertTemperature(100.0, TemperatureUnit.CELSIUS, TemperatureUnit.KELVIN),
            delta,
        )

    // ── Temperature: K ↔ F ──

    @Test
    fun tempKToF_absoluteZero() =
        assertEquals(
            -459.67,
            converter.convertTemperature(0.0, TemperatureUnit.KELVIN, TemperatureUnit.FAHRENHEIT),
            delta,
        )

    @Test
    fun tempKToF_boiling() =
        assertEquals(
            212.0,
            converter.convertTemperature(373.15, TemperatureUnit.KELVIN, TemperatureUnit.FAHRENHEIT),
            delta,
        )

    // ── Temperature: identity ──

    @Test
    fun tempCToC() =
        assertEquals(
            37.5,
            converter.convertTemperature(37.5, TemperatureUnit.CELSIUS, TemperatureUnit.CELSIUS),
            delta,
        )

    @Test
    fun tempFToF() =
        assertEquals(
            98.6,
            converter.convertTemperature(98.6, TemperatureUnit.FAHRENHEIT, TemperatureUnit.FAHRENHEIT),
            delta,
        )

    @Test
    fun tempKToK() =
        assertEquals(
            300.0,
            converter.convertTemperature(300.0, TemperatureUnit.KELVIN, TemperatureUnit.KELVIN),
            delta,
        )

    // ── Area ──

    @Test
    fun areaSqmToSqft() =
        assertEquals(
            10.76391,
            converter.convertArea(1.0, AreaUnit.SQUARE_METERS, AreaUnit.SQUARE_FEET),
            1e-3,
        )

    @Test
    fun areaSqftToSqm() =
        assertEquals(
            1.0,
            converter.convertArea(10.76391, AreaUnit.SQUARE_FEET, AreaUnit.SQUARE_METERS),
            1e-3,
        )

    @Test
    fun areaAcresToHectares() =
        assertEquals(
            0.404686,
            converter.convertArea(1.0, AreaUnit.ACRES, AreaUnit.HECTARES),
            1e-4,
        )

    @Test
    fun areaHectaresToAcres() =
        assertEquals(
            2.47105,
            converter.convertArea(1.0, AreaUnit.HECTARES, AreaUnit.ACRES),
            1e-3,
        )

    @Test
    fun areaSqkmToSqm() =
        assertEquals(
            1_000_000.0,
            converter.convertArea(1.0, AreaUnit.SQUARE_KILOMETERS, AreaUnit.SQUARE_METERS),
            delta,
        )

    @Test
    fun areaSqmToSqkm() =
        assertEquals(
            1.0,
            converter.convertArea(1_000_000.0, AreaUnit.SQUARE_METERS, AreaUnit.SQUARE_KILOMETERS),
            delta,
        )

    @Test
    fun areaIdentity() =
        assertEquals(
            42.0,
            converter.convertArea(42.0, AreaUnit.ACRES, AreaUnit.ACRES),
            delta,
        )

    // ── Round-trip precision ──

    @Test
    fun roundTripLength() {
        val original = 12.345
        val km = converter.convertLength(original, LengthUnit.MILES, LengthUnit.KILOMETERS)
        val back = converter.convertLength(km, LengthUnit.KILOMETERS, LengthUnit.MILES)
        assertEquals(original, back, delta)
    }

    @Test
    fun roundTripMass() {
        val original = 5.678
        val g = converter.convertMass(original, MassUnit.POUNDS, MassUnit.GRAMS)
        val back = converter.convertMass(g, MassUnit.GRAMS, MassUnit.POUNDS)
        assertEquals(original, back, delta)
    }

    @Test
    fun roundTripArea() {
        val original = 3.14
        val sqft = converter.convertArea(original, AreaUnit.HECTARES, AreaUnit.SQUARE_FEET)
        val back = converter.convertArea(sqft, AreaUnit.SQUARE_FEET, AreaUnit.HECTARES)
        assertEquals(original, back, delta)
    }

    @Test
    fun roundTripTemp() {
        val original = 37.0
        val f = converter.convertTemperature(original, TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT)
        val back = converter.convertTemperature(f, TemperatureUnit.FAHRENHEIT, TemperatureUnit.CELSIUS)
        assertEquals(original, back, delta)
    }

    // ── Very large values ──

    @Test
    fun largeKmToM() =
        assertEquals(
            1e9,
            converter.convertLength(1_000_000.0, LengthUnit.KILOMETERS, LengthUnit.METERS),
            1.0,
        )

    @Test
    fun largeSqmToSqkm() =
        assertEquals(
            1.0,
            converter.convertArea(1e6, AreaUnit.SQUARE_METERS, AreaUnit.SQUARE_KILOMETERS),
            delta,
        )

    // ── Very small values ──

    @Test
    fun smallMmToM() =
        assertEquals(
            0.000001,
            converter.convertLength(0.001, LengthUnit.MILLIMETERS, LengthUnit.METERS),
            1e-12,
        )

    @Test
    fun smallGToKg() =
        assertEquals(
            0.001,
            converter.convertMass(1.0, MassUnit.GRAMS, MassUnit.KILOGRAMS),
            delta,
        )

    // ── Zero values ──

    @Test
    fun zeroLength() =
        assertEquals(
            0.0,
            converter.convertLength(0.0, LengthUnit.MILES, LengthUnit.METERS),
            delta,
        )

    @Test
    fun zeroMass() =
        assertEquals(
            0.0,
            converter.convertMass(0.0, MassUnit.POUNDS, MassUnit.KILOGRAMS),
            delta,
        )

    @Test
    fun zeroArea() =
        assertEquals(
            0.0,
            converter.convertArea(0.0, AreaUnit.ACRES, AreaUnit.HECTARES),
            delta,
        )

    @Test
    fun zeroTempCToF() =
        assertEquals(
            32.0,
            converter.convertTemperature(0.0, TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT),
            delta,
        )

    // ── Negative values ──

    @Test
    fun negativeTempCToF() =
        assertEquals(
            14.0,
            converter.convertTemperature(-10.0, TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT),
            delta,
        )

    @Test
    fun negativeLength() =
        assertEquals(
            -1000.0,
            converter.convertLength(-1.0, LengthUnit.KILOMETERS, LengthUnit.METERS),
            delta,
        )
}
