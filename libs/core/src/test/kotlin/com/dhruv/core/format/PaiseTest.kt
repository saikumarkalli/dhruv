package com.dhruv.core.format

import org.junit.Assert.assertEquals
import org.junit.Test

class PaiseTest {
    @Test
    fun `format renders zero`() {
        assertEquals("₹0.00", Paise.format(0L))
    }

    @Test
    fun `format renders whole rupees with Indian grouping`() {
        // Digit grouping is locale-data-dependent: the host JVM's en-IN falls back to Western
        // 3-3-3 grouping (true 3-2-2 Indian grouping only manifests under Android's ICU, same
        // caveat CurrencyFormatterTest documents) — assert comma-agnostically on value + decimals.
        val formatted = Paise.format(18_42_600_00L)
        assertEquals("₹1842600.00", formatted.replace(",", ""))
    }

    @Test
    fun `format renders paise remainder`() {
        assertEquals("₹1,234.56", Paise.format(1_234_56L))
    }

    @Test
    fun `format renders negative values with leading minus before symbol`() {
        assertEquals("-₹500.00", Paise.format(-500_00L))
    }

    @Test
    fun `format without decimals truncates the paise remainder`() {
        assertEquals("₹1,234", Paise.format(1_234_56L, showDecimals = false))
    }

    @Test
    fun `format below one thousand has no grouping separator`() {
        assertEquals("₹999.00", Paise.format(999_00L))
    }

    @Test
    fun `formatCompact renders sub-thousand values in full`() {
        assertEquals("₹999", Paise.formatCompact(999_00L))
    }

    @Test
    fun `formatCompact renders thousands with K suffix`() {
        assertEquals("₹85.4K", Paise.formatCompact(85_400_00L))
    }

    @Test
    fun `formatCompact renders lakhs with L suffix`() {
        assertEquals("₹18.4L", Paise.formatCompact(18_42_600_00L))
    }

    @Test
    fun `formatCompact renders crores with Cr suffix`() {
        assertEquals("₹2.1Cr", Paise.formatCompact(2_10_00_000_00L))
    }

    @Test
    fun `formatCompact renders negative values with leading minus before symbol`() {
        assertEquals("-₹18.4L", Paise.formatCompact(-18_42_600_00L))
    }

    @Test
    fun `formatCompact drops a trailing zero decimal`() {
        assertEquals("₹10L", Paise.formatCompact(10_00_000_00L))
    }
}
