package com.example.util

interface IUnitConverter {
    /**
     * Converts a value from one length unit to another.
     */
    fun convertLength(value: Double, from: LengthUnit, to: LengthUnit): Double

    /**
     * Converts a value from one mass unit to another.
     */
    fun convertMass(value: Double, from: MassUnit, to: MassUnit): Double
}
