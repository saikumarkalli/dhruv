package com.dhruv.core.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `SET-BR-019` / FR-025: with hide-amounts on, the money formatting path masks values. The mask is
 * a fixed-width token (`₹••••`) regardless of magnitude, sign, or [Paise.formatCompact] vs
 * [Paise.format] — a mask whose length varies with the real value would itself leak the value's
 * digit count, defeating the point (flagged during 0b.3's security checklist review, `CHK034`,
 * for the *AI-key* masking case; the same reasoning is applied here from the start rather than
 * discovered the same way twice).
 */
class HideAmountsTest {
    @Test
    fun `format masks to a fixed token regardless of magnitude`() {
        val small = Paise.format(paise = 100L, masked = true)
        val large = Paise.format(paise = 18_42_60_000_00L, masked = true)
        assertEquals(small, large)
        assertEquals(Paise.MASKED_TOKEN, small)
    }

    @Test
    fun `format masks negative amounts the same as positive`() {
        assertEquals(Paise.MASKED_TOKEN, Paise.format(paise = -50_00L, masked = true))
    }

    @Test
    fun `formatCompact also masks to the same fixed token`() {
        assertEquals(Paise.MASKED_TOKEN, Paise.formatCompact(paise = 18_42_60_000_00L, masked = true))
    }

    @Test
    fun `unmasked formatting is unchanged from before hide-amounts existed`() {
        assertEquals("₹1,000.00", Paise.format(paise = 1_000_00L, masked = false))
        assertTrue(Paise.formatCompact(paise = 18_42_600_00L, masked = false).endsWith("L"))
    }
}
