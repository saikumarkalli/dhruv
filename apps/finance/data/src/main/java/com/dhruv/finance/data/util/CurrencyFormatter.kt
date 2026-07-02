package com.dhruv.finance.data.util

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    /**
     * Formats a BigDecimal value into Indian numbering system layout (e.g. ₹ 1,00,000.00).
     */
    fun format(
        value: BigDecimal,
        symbol: String = "₹",
    ): String {
        val roundedValue = value.setScale(2, java.math.RoundingMode.HALF_UP)
        val formatter = NumberFormat.getNumberInstance(Locale("en", "IN")) as DecimalFormat

        // Ensure 2 decimal places are always rendered for standard financial reporting
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2

        val formattedNumber = formatter.format(roundedValue)
        return if (symbol.endsWith(" ")) "$symbol$formattedNumber" else "$symbol $formattedNumber"
    }

    /**
     * Helper to format double values.
     */
    fun format(
        value: Double,
        symbol: String = "₹",
    ): String {
        if (value.isNaN() || value.isInfinite()) return "$symbol 0.00"
        return format(BigDecimal(value.toString()), symbol)
    }
}
