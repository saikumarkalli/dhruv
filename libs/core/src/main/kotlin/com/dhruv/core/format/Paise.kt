package com.dhruv.core.format

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/**
 * The single money renderer for integer-paise amounts (ADR-0014 §4). Pure — no Compose import —
 * so it is usable from non-Compose surfaces (notifications, widgets) as well as [MoneyText]
 * (`com.dhruv.core.ui.components`).
 */
object Paise {
    private const val SYMBOL = "₹"

    /** Full Indian-grouped amount, e.g. `₹18,42,600.00` (or `₹18,42,600` if [showDecimals] is false). */
    fun format(
        paise: Long,
        showDecimals: Boolean = true,
    ): String {
        // NumberFormat (not a hand-built DecimalFormat pattern) is required: only the locale's own
        // grouping data reliably alternates 3-then-2 digits for Indian numbering on the host JVM.
        val formatter = NumberFormat.getNumberInstance(Locale("en", "IN")) as DecimalFormat
        val fractionDigits = if (showDecimals) 2 else 0
        formatter.minimumFractionDigits = fractionDigits
        formatter.maximumFractionDigits = fractionDigits
        formatter.roundingMode = RoundingMode.DOWN

        var rupees = BigDecimal(abs(paise)).movePointLeft(2)
        if (!showDecimals) rupees = rupees.setScale(0, RoundingMode.DOWN)

        val amount = formatter.format(rupees)
        return if (paise < 0) "-$SYMBOL$amount" else "$SYMBOL$amount"
    }

    /** Compact amount using Indian K/L/Cr suffixes, e.g. `₹85.4K`, `₹18.4L`, `₹2.1Cr`. */
    fun formatCompact(paise: Long): String {
        val magnitude = abs(paise)
        val rupees = BigDecimal(magnitude).movePointLeft(2)
        val sign = if (paise < 0) "-" else ""

        val (divisor, suffix) =
            when {
                magnitude >= CRORE_PAISE -> CRORE_PAISE to "Cr"
                magnitude >= LAKH_PAISE -> LAKH_PAISE to "L"
                magnitude >= THOUSAND_PAISE -> THOUSAND_PAISE to "K"
                else -> return "$sign$SYMBOL${rupees.setScale(0, RoundingMode.DOWN)}"
            }

        val scaled = rupees.divide(BigDecimal(divisor).movePointLeft(2), 1, RoundingMode.DOWN)
        val trimmed = scaled.stripTrailingZeros().toPlainString()
        return "$sign$SYMBOL$trimmed$suffix"
    }

    private const val THOUSAND_PAISE = 1_000_00L
    private const val LAKH_PAISE = 1_00_000_00L
    private const val CRORE_PAISE = 1_00_00_000_00L
}
