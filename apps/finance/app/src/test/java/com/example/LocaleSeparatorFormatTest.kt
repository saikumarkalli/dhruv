package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for the locale-separator formatting logic mirrored from
 * [CalculatorViewModel.formatLocaleSeparator].
 *
 * We test the pure algorithm inline rather than through the ViewModel
 * to avoid needing Android/Mockito dependencies in unit tests.
 *
 * Coverage:
 *  • International format (standard 3-digit groups)
 *  • Indian format (last-3 then 2-digit groups)
 *  • Negative numbers, decimal values, zero
 *  • Error / NaN / Infinity / scientific-notation passthrough
 *  • Empty string passthrough
 */
class LocaleSeparatorFormatTest {

    // ── International (standard) format ─────────────────────────────────────

    @Test fun intl_1digit()     = assertEquals("5",             fmt("5", "international"))
    @Test fun intl_3digits()    = assertEquals("999",           fmt("999", "international"))
    @Test fun intl_4digits()    = assertEquals("1,000",         fmt("1000", "international"))
    @Test fun intl_6digits()    = assertEquals("100,000",       fmt("100000", "international"))
    @Test fun intl_7digits()    = assertEquals("1,234,567",     fmt("1234567", "international"))
    @Test fun intl_10digits()   = assertEquals("1,234,567,890", fmt("1234567890", "international"))
    @Test fun intl_negative()   = assertEquals("-1,000",        fmt("-1000", "international"))
    @Test fun intl_decimal()    = assertEquals("1,234.56",      fmt("1234.56", "international"))
    @Test fun intl_zero()       = assertEquals("0",             fmt("0", "international"))
    @Test fun intl_exactlyThree()= assertEquals("123",          fmt("123", "international"))

    // ── Indian format ────────────────────────────────────────────────────────

    @Test fun indian_1digit()   = assertEquals("5",           fmt("5", "indian"))
    @Test fun indian_3digits()  = assertEquals("999",         fmt("999", "indian"))
    @Test fun indian_4digits()  = assertEquals("1,000",       fmt("1000", "indian"))
    @Test fun indian_5digits()  = assertEquals("12,000",      fmt("12000", "indian"))
    @Test fun indian_6digits()  = assertEquals("1,00,000",    fmt("100000", "indian"))
    @Test fun indian_7digits()  = assertEquals("12,34,567",   fmt("1234567", "indian"))
    @Test fun indian_8digits()  = assertEquals("1,23,45,678", fmt("12345678", "indian"))
    @Test fun indian_negative() = assertEquals("-1,00,000",   fmt("-100000", "indian"))
    @Test fun indian_decimal()  = assertEquals("12,34,567.89",fmt("1234567.89", "indian"))
    @Test fun indian_zero()     = assertEquals("0",            fmt("0", "indian"))

    // ── Passthrough — unchanged strings ─────────────────────────────────────

    @Test fun passthrough_empty()       = assertEquals("",           fmt("", "international"))
    @Test fun passthrough_error()       = assertEquals("Error",      fmt("Error", "international"))
    @Test fun passthrough_errorMsg()    = assertEquals("Error: ÷0",  fmt("Error: ÷0", "indian"))
    @Test fun passthrough_nan()         = assertEquals("NaN",        fmt("NaN", "international"))
    @Test fun passthrough_infinity()    = assertEquals("Infinity",   fmt("Infinity", "international"))
    @Test fun passthrough_negInf()      = assertEquals("-Infinity",  fmt("-Infinity", "indian"))
    @Test fun passthrough_sciCapE()     = assertEquals("1.23E5",     fmt("1.23E5", "international"))
    @Test fun passthrough_sciLowE()     = assertEquals("1.5e-3",     fmt("1.5e-3", "indian"))

    // ─────────────────────────────────────────────────────────────────────────
    // Standalone pure-function replica (mirrors CalculatorViewModel.formatLocaleSeparator)
    // ─────────────────────────────────────────────────────────────────────────

    private fun fmt(numberStr: String, format: String): String {
        if (numberStr.isEmpty() || numberStr == "Error" || numberStr.startsWith("Error")) return numberStr
        if (numberStr.contains("E") || numberStr.contains("e") ||
            numberStr.contains("Infinity") || numberStr.contains("NaN")
        ) return numberStr

        val parts = numberStr.split(".")
        val intPart = parts[0]
        val isNegative = intPart.startsWith("-")
        val cleanInt = if (isNegative) intPart.substring(1) else intPart

        val formattedInt = if (format == "indian") {
            if (cleanInt.length <= 3) {
                cleanInt
            } else {
                val last3 = cleanInt.substring(cleanInt.length - 3)
                val remaining = cleanInt.substring(0, cleanInt.length - 3)
                val sb = StringBuilder()
                var count = 0
                for (i in remaining.length - 1 downTo 0) {
                    sb.insert(0, remaining[i])
                    count++
                    if (count == 2 && i > 0) {
                        sb.insert(0, ',')
                        count = 0
                    }
                }
                sb.append(',').append(last3).toString()
            }
        } else {
            val sb = StringBuilder()
            var count = 0
            for (i in cleanInt.length - 1 downTo 0) {
                sb.insert(0, cleanInt[i])
                count++
                if (count == 3 && i > 0) {
                    sb.insert(0, ',')
                    count = 0
                }
            }
            sb.toString()
        }

        val finalInt = if (isNegative) "-$formattedInt" else formattedInt
        return if (parts.size > 1) "$finalInt.${parts[1]}" else finalInt
    }
}
