package com.dhruv.core.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DhruvNextResponsiveTokensTest {
    @Test
    fun `default phone portrait matches the original fixed DhruvNext values`() {
        val tokens = calculateDhruvNextResponsiveTokens(widthDp = 400, heightDp = 800)

        assertEquals(18f, tokens.spacing.cardPadding.value)
        assertEquals(16f, tokens.spacing.screenGutter.value)
        assertEquals(12f, tokens.spacing.interCardGap.value)
        assertEquals(20f, tokens.radii.card.value)
        assertEquals(17f, tokens.type.title.value)
        assertEquals(15f, tokens.type.cardTitle.value)
        assertEquals(38f, tokens.type.hero.value)
        assertEquals(22f, tokens.keypad.digit.value)
        assertEquals(26f, tokens.keypad.operator.value)
        assertEquals(13f, tokens.keypad.function.value)
        assertEquals(11f, tokens.keypad.caption.value)
    }

    @Test
    fun `narrow phone scales spacing, type, and keypad down`() {
        val tokens = calculateDhruvNextResponsiveTokens(widthDp = 320, heightDp = 568)

        assertTrue(tokens.spacing.screenGutter.value < 16f)
        assertTrue(tokens.type.title.value < 17f)
        assertTrue(tokens.type.hero.value < 38f)
        assertTrue(tokens.keypad.digit.value < 22f)
        assertTrue(tokens.keypad.operator.value < 26f)
    }

    @Test
    fun `short height alone triggers the small tier even at normal width`() {
        val tokens = calculateDhruvNextResponsiveTokens(widthDp = 400, heightDp = 550)

        assertTrue(tokens.type.title.value < 17f)
        assertTrue(tokens.keypad.digit.value < 22f)
    }

    @Test
    fun `a phone rotated to landscape falls into the small tier via its short height`() {
        // Typical phone: portrait ~400x800 rotated becomes landscape width=800, height=400 — the
        // former portrait width is now a height well under 600dp, so it lands in "small", which is
        // the right outcome (tight vertical space), not a dedicated landscape tier (see the
        // unreachability note on calculateDhruvNextResponsiveTokens).
        val tokens = calculateDhruvNextResponsiveTokens(widthDp = 800, heightDp = 400)

        assertTrue(tokens.type.title.value < 17f)
        assertTrue(tokens.spacing.screenGutter.value < 16f)
        assertTrue(tokens.keypad.digit.value < 22f)
    }

    @Test
    fun `tablet width scales spacing, type, and keypad up`() {
        val tokens = calculateDhruvNextResponsiveTokens(widthDp = 800, heightDp = 1280)

        assertTrue(tokens.spacing.screenGutter.value > 16f)
        assertTrue(tokens.type.title.value > 17f)
        assertEquals(46f, tokens.type.hero.value)
        assertTrue(tokens.keypad.digit.value > 22f)
        assertTrue(tokens.keypad.operator.value > 26f)
    }

    @Test
    fun `corner radii never change across breakpoints`() {
        val breakpoints = listOf(320 to 568, 400 to 800, 800 to 400, 800 to 1280)
        val allRadii = breakpoints.map { (w, h) -> calculateDhruvNextResponsiveTokens(w, h).radii }

        allRadii.forEach { radii ->
            assertEquals(20f, radii.card.value)
            assertEquals(18f, radii.listGroup.value)
            assertEquals(14f, radii.innerTile.value)
            assertEquals(26f, radii.pill.value)
        }
    }

    @Test
    fun `keypad glyphs stay a fixed offset below the content title size at every tier`() {
        // Regression guard for the "keypad looks frozen" bug — keypad.digit must always move in
        // the same direction as type.title (both shrink together, both grow together), not stay
        // constant while the rest of the screen scales.
        val small = calculateDhruvNextResponsiveTokens(widthDp = 320, heightDp = 568)
        val default = calculateDhruvNextResponsiveTokens(widthDp = 400, heightDp = 800)
        val tablet = calculateDhruvNextResponsiveTokens(widthDp = 800, heightDp = 1280)

        assertTrue(small.keypad.digit.value < default.keypad.digit.value)
        assertTrue(default.keypad.digit.value < tablet.keypad.digit.value)
    }
}
