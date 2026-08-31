package com.dhruv.finance.data.util

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    /** Hide-amounts mask (FR-025) for non-Compose surfaces (notifications, widget) — same fixed-width
     * token as `com.dhruv.core.format.Paise.MASKED_TOKEN`, kept as a literal here since :apps:finance:data
     * does not depend on :libs:core's UI-adjacent format package. */
    const val MASKED_TOKEN = "₹••••"

    /**
     * Formats a BigDecimal value into Indian numbering system layout (e.g. ₹ 1,00,000.00).
     * When [masked] is true, every other parameter is ignored and [MASKED_TOKEN] is returned.
     */
    fun format(
        value: BigDecimal,
        symbol: String = "₹",
        masked: Boolean = false,
    ): String {
        if (masked) return MASKED_TOKEN
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
