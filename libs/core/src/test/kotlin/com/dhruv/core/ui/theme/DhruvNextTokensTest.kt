package com.dhruv.core.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DhruvNextTokensTest {
    @Test
    fun `null hex returns the light base palette unchanged`() {
        val resolved = resolveDhruvNextColors(darkTheme = false, accentColorHex = null)
        assertEquals(DhruvNextLightColors, resolved)
    }

    @Test
    fun `null hex returns the dark base palette unchanged`() {
        val resolved = resolveDhruvNextColors(darkTheme = true, accentColorHex = null)
        assertEquals(DhruvNextDarkColors, resolved)
    }

    @Test
    fun `invalid hex falls back to the base palette instead of crashing`() {
        val resolved = resolveDhruvNextColors(darkTheme = false, accentColorHex = "not-a-color")
        assertEquals(DhruvNextLightColors, resolved)
    }

    @Test
    fun `dark accent hex gets white onAcc`() {
        // Polar Blue light primary — dark/saturated enough to need light text on top.
        val resolved = resolveDhruvNextColors(darkTheme = false, accentColorHex = "#0061A4")
        assertEquals(Color(0xFF0061A4), resolved.acc)
        assertEquals(DhruvNextLightColors.onAcc, resolved.onAcc)
    }

    @Test
    fun `light accent hex gets dark onAcc`() {
        // Polar Blue dark primary — pale, needs dark text on top instead of white.
        val resolved = resolveDhruvNextColors(darkTheme = true, accentColorHex = "#D3E3FD")
        assertNotEquals(DhruvNextDarkColors.onAcc, resolved.onAcc)
    }

    @Test
    fun `overridden accent still derives soft and line tints`() {
        val resolved = resolveDhruvNextColors(darkTheme = false, accentColorHex = "#4A148C")
        assertNotEquals(DhruvNextLightColors.accSoft, resolved.accSoft)
        assertNotEquals(DhruvNextLightColors.accLine, resolved.accLine)
    }
}
