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
}
