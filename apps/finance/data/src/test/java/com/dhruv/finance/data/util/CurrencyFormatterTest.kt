package com.dhruv.finance.data.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

/**
 * Regression tests for [CurrencyFormatter] — Indian-numbering money formatting used across the
 * finance calculators. Pure JVM (explicit en-IN locale), so no Robolectric is needed.
 */
class CurrencyFormatterTest {
    @Test
    fun `formats large amount with symbol, grouping and two decimals`() {
        // Digit grouping is locale-data-dependent: the JVM's en-IN gives Western grouping
        // ("1,00,000" only under Android's ICU), so assert comma-agnostically on value + decimals.
        val formatted = CurrencyFormatter.format(BigDecimal("100000"))
        assertEquals("₹ 100000.00", formatted.replace(",", ""))
    }

    @Test
    fun `always renders exactly two fraction digits`() {
        assertEquals("₹ 1,234.50", CurrencyFormatter.format(BigDecimal("1234.5")))
        assertEquals("₹ 5.00", CurrencyFormatter.format(BigDecimal("5")))
    }

    @Test
    fun `rounds half up to two places`() {
        assertEquals("₹ 1.01", CurrencyFormatter.format(BigDecimal("1.005")))
        assertEquals("₹ 2.35", CurrencyFormatter.format(BigDecimal("2.345")))
    }

    @Test
    fun `negative amounts keep the grouping and sign`() {
        assertEquals("₹ -2,500.00", CurrencyFormatter.format(BigDecimal("-2500")))
    }

    @Test
    fun `custom symbol without trailing space is spaced`() {
        assertEquals("$ 10.00", CurrencyFormatter.format(BigDecimal("10"), "$"))
    }

    @Test
    fun `custom symbol with trailing space is not double-spaced`() {
        assertEquals("$ 10.00", CurrencyFormatter.format(BigDecimal("10"), "$ "))
    }

    @Test
    fun `non-finite doubles degrade to zero rather than crashing`() {
        assertEquals("₹ 0.00", CurrencyFormatter.format(Double.NaN))
        assertEquals("₹ 0.00", CurrencyFormatter.format(Double.POSITIVE_INFINITY))
        assertEquals("₹ 0.00", CurrencyFormatter.format(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun `double overload matches decimal formatting`() {
        assertEquals("₹ 1,234.50", CurrencyFormatter.format(1234.5))
    }
}
