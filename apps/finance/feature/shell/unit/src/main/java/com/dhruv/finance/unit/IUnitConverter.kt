package com.dhruv.finance.unit

interface IUnitConverter {
    /**
     * Converts a value from one length unit to another.
     */
    fun convertLength(
        value: Double,
        from: LengthUnit,
        to: LengthUnit,
    ): Double

    /**
     * Converts a value from one mass unit to another.
     */
    fun convertMass(
        value: Double,
        from: MassUnit,
        to: MassUnit,
    ): Double

    /**
     * Converts a value from one area unit to another.
     */
    fun convertArea(
        value: Double,
        from: AreaUnit,
        to: AreaUnit,
    ): Double

    /**
     * Converts a value from one temperature unit to another. Temperature is affine (°C/°F/K have
     * different zero points), so this is NOT a ratio conversion — see [TemperatureUnit].
     */
    fun convertTemperature(
        value: Double,
        from: TemperatureUnit,
        to: TemperatureUnit,
    ): Double
}
