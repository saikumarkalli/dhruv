package com.dhruv.core.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class InitialsForTest {
    @Test
    fun `two words take first letter of each`() {
        assertEquals("SK", initialsFor("Sai Kumar"))
    }

    @Test
    fun `single word takes its first two letters`() {
        assertEquals("AM", initialsFor("Amazon"))
    }

    @Test
    fun `single letter word returns just that letter`() {
        assertEquals("A", initialsFor("A"))
    }

    @Test
    fun `blank name returns empty string`() {
        assertEquals("", initialsFor("   "))
    }

    @Test
    fun `extra whitespace between words is ignored`() {
        assertEquals("SK", initialsFor("  Sai   Kumar  "))
    }

    @Test
    fun `more than two words only uses the first two`() {
        assertEquals("SK", initialsFor("Sai Kumar Reddy"))
    }
}
