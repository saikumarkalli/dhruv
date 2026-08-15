package com.dhruv.finance.unit

enum class LengthUnit(
    val label: String,
    val symbol: String,
    val ratioToBase: Double,
) {
    METERS("Meters", "m", 1.0),
    KILOMETERS("Kilometers", "km", 1000.0),
    CENTIMETERS("Centimeters", "cm", 0.01),
    MILLIMETERS("Millimeters", "mm", 0.001),
    INCHES("Inches", "in", 0.0254),
    FEET("Feet", "ft", 0.3048),
    YARDS("Yards", "yd", 0.9144),
    MILES("Miles", "mi", 1609.344),
    ;

    companion object {
        fun convert(
            value: Double,
            from: LengthUnit,
            to: LengthUnit,
        ): Double {
            val baseValue = value * from.ratioToBase
            return baseValue / to.ratioToBase
        }
    }
}

enum class MassUnit(
    val label: String,
    val symbol: String,
    val ratioToBase: Double,
) {
    KILOGRAMS("Kilograms", "kg", 1.0),
    GRAMS("Grams", "g", 0.001),
    MILLIGRAMS("Milligrams", "mg", 0.000001),
    POUNDS("Pounds", "lb", 0.45359237),
    OUNCES("Ounces", "oz", 0.028349523125),
    ;

    companion object {
        fun convert(
            value: Double,
            from: MassUnit,
            to: MassUnit,
        ): Double {
            val baseValue = value * from.ratioToBase
            return baseValue / to.ratioToBase
        }
    }
}

enum class AreaUnit(
    val label: String,
    val symbol: String,
    val ratioToBase: Double,
) {
    SQUARE_METERS("Square Meters", "m²", 1.0),
    SQUARE_FEET("Square Feet", "ft²", 0.09290304),
    SQUARE_KILOMETERS("Square Kilometers", "km²", 1_000_000.0),
    ACRES("Acres", "ac", 4046.8564224),
    HECTARES("Hectares", "ha", 10_000.0),
    ;

    companion object {
        fun convert(
            value: Double,
            from: AreaUnit,
            to: AreaUnit,
        ): Double {
            val baseValue = value * from.ratioToBase
            return baseValue / to.ratioToBase
        }
    }
}

/**
 * Temperature is affine, not ratio-based: °C, °F and K have different zero points, so a value
 * cannot be expressed as `value * ratioToBase` against a shared base the way [LengthUnit],
 * [MassUnit] and [AreaUnit] are. Every conversion pivots through Celsius instead.
 *
 * This converter does not validate physical plausibility (e.g. it will not reject or clamp a
 * result below absolute zero, -273.15°C / -459.67°F / 0K) — it just computes the arithmetic
 * result, the same way [LengthUnit.convert]/[MassUnit.convert] don't reject negative
 * lengths/masses (see `lengthNegative`/`massNegative` in ConverterAndFormatterTest). Kept
 * consistent with the rest of this converter rather than special-cased.
 */
enum class TemperatureUnit(
    val label: String,
    val symbol: String,
) {
    CELSIUS("Celsius", "°C"),
    FAHRENHEIT("Fahrenheit", "°F"),
    KELVIN("Kelvin", "K"),
    ;

    companion object {
        private fun celsiusToFahrenheit(celsius: Double): Double = celsius * 9.0 / 5.0 + 32.0

        private fun fahrenheitToCelsius(fahrenheit: Double): Double = (fahrenheit - 32.0) * 5.0 / 9.0

        private fun celsiusToKelvin(celsius: Double): Double = celsius + 273.15

        private fun kelvinToCelsius(kelvin: Double): Double = kelvin - 273.15

        /**
         * Converts [value] from [from] to [to], pivoting through Celsius. Same-unit conversions
         * are a no-op that returns [value] unchanged — never round-tripped through the pivot,
         * which would introduce needless floating-point drift.
         */
        fun convert(
            value: Double,
            from: TemperatureUnit,
            to: TemperatureUnit,
        ): Double {
            if (from == to) return value
            val celsius =
                when (from) {
                    CELSIUS -> value
                    FAHRENHEIT -> fahrenheitToCelsius(value)
                    KELVIN -> kelvinToCelsius(value)
                }
            return when (to) {
                CELSIUS -> celsius
                FAHRENHEIT -> celsiusToFahrenheit(celsius)
                KELVIN -> celsiusToKelvin(celsius)
            }
        }
    }
}

/**
 * Identifies a unit category (Length/Mass/Area/Temperature) and knows how to convert within it,
 * so callers — namely Task 5's "12.5 km is also …" multi-unit list — can work with any category
 * through one uniform shape, regardless of whether that category is ratio-based (Length/Mass/Area)
 * or affine (Temperature).
 */
sealed interface UnitCategory<U> {
    val units: List<U>

    fun convert(
        value: Double,
        from: U,
        to: U,
    ): Double

    data object Length : UnitCategory<LengthUnit> {
        override val units: List<LengthUnit> = LengthUnit.entries

        override fun convert(
            value: Double,
            from: LengthUnit,
            to: LengthUnit,
        ): Double = LengthUnit.convert(value, from, to)
    }

    data object Mass : UnitCategory<MassUnit> {
        override val units: List<MassUnit> = MassUnit.entries

        override fun convert(
            value: Double,
            from: MassUnit,
            to: MassUnit,
        ): Double = MassUnit.convert(value, from, to)
    }

    data object Area : UnitCategory<AreaUnit> {
        override val units: List<AreaUnit> = AreaUnit.entries

        override fun convert(
            value: Double,
            from: AreaUnit,
            to: AreaUnit,
        ): Double = AreaUnit.convert(value, from, to)
    }

    data object Temperature : UnitCategory<TemperatureUnit> {
        override val units: List<TemperatureUnit> = TemperatureUnit.entries

        override fun convert(
            value: Double,
            from: TemperatureUnit,
            to: TemperatureUnit,
        ): Double = TemperatureUnit.convert(value, from, to)
    }
}

/**
 * Given [value] currently expressed in [from], returns it expressed in every *other* unit of this
 * category (i.e. [from] itself is excluded — the caller already has that value). Backs Task 5's
 * "12.5 km is also 7.76 mi, 12500 m, …" list: one call works identically whether `this` is
 * [UnitCategory.Length], [UnitCategory.Mass], [UnitCategory.Area] or [UnitCategory.Temperature],
 * because the affine-vs-ratio difference is already resolved inside each category's [UnitCategory.convert].
 *
 * Order matches [UnitCategory.units] (i.e. enum declaration order) with [from] filtered out.
 */
fun <U> UnitCategory<U>.convertToOtherUnits(
    value: Double,
    from: U,
): List<Pair<U, Double>> =
    units
        .filter { it != from }
        .map { unit -> unit to convert(value, from, unit) }

object UnitConverter : IUnitConverter {
    override fun convertLength(
        value: Double,
        from: LengthUnit,
        to: LengthUnit,
    ): Double = LengthUnit.convert(value, from, to)

    override fun convertMass(
        value: Double,
        from: MassUnit,
        to: MassUnit,
    ): Double = MassUnit.convert(value, from, to)

    override fun convertArea(
        value: Double,
        from: AreaUnit,
        to: AreaUnit,
    ): Double = AreaUnit.convert(value, from, to)

    override fun convertTemperature(
        value: Double,
        from: TemperatureUnit,
        to: TemperatureUnit,
    ): Double = TemperatureUnit.convert(value, from, to)
}
