package com.dhruv.core.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatBadgeCountTest {
    @Test
    fun `count within cap renders exactly`() {
        assertEquals("42", formatBadgeCount(42))
    }

    @Test
    fun `count exactly at cap renders exactly`() {
        assertEquals("99", formatBadgeCount(99))
    }

    @Test
    fun `count over cap renders as 99+`() {
        assertEquals("99+", formatBadgeCount(142))
    }

    @Test
    fun `zero renders as zero`() {
        assertEquals("0", formatBadgeCount(0))
    }
}
