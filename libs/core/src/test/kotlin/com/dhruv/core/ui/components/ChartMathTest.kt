package com.dhruv.core.ui.components

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

private const val FLOAT_TOLERANCE = 0.001f

private fun assertFloatListEquals(
    expected: List<Float>,
    actual: List<Float>,
) {
    assertEquals(expected.size, actual.size)
    expected.forEachIndexed { index, value ->
        assertEquals(value, actual[index], FLOAT_TOLERANCE)
    }
}

class ChartMathTest {
    @Test
    fun `sparklinePoints of empty list is empty`() {
        assertEquals(emptyList<Offset>(), sparklinePoints(emptyList(), width = 100f, height = 50f))
    }

    @Test
    fun `sparklinePoints of a single value centers it`() {
        assertEquals(listOf(Offset(50f, 25f)), sparklinePoints(listOf(7f), width = 100f, height = 50f))
    }

    @Test
    fun `sparklinePoints maps min to bottom and max to top`() {
        val points = sparklinePoints(listOf(0f, 5f, 10f), width = 100f, height = 50f)
        assertEquals(listOf(Offset(0f, 50f), Offset(50f, 25f), Offset(100f, 0f)), points)
    }

    @Test
    fun `sparklinePoints of a flat series draws a horizontal mid-line`() {
        val points = sparklinePoints(listOf(4f, 4f, 4f), width = 90f, height = 40f)
        assertEquals(listOf(Offset(0f, 20f), Offset(45f, 20f), Offset(90f, 20f)), points)
    }

    @Test
    fun `allocationPercentages splits proportionally`() {
        assertFloatListEquals(listOf(50f, 30f, 20f), allocationPercentages(listOf(50f, 30f, 20f)))
    }

    @Test
    fun `allocationPercentages of all-zero values returns zeros not NaN`() {
        assertFloatListEquals(listOf(0f, 0f), allocationPercentages(listOf(0f, 0f)))
    }

    @Test
    fun `allocationPercentages of equal values splits evenly`() {
        assertFloatListEquals(listOf(50f, 50f), allocationPercentages(listOf(1f, 1f)))
    }

    @Test
    fun `barHeights scales relative to the tallest bar`() {
        assertFloatListEquals(listOf(50f, 100f, 25f), barHeights(listOf(10f, 20f, 5f), maxHeightPx = 100f))
    }

    @Test
    fun `barHeights of empty list is empty`() {
        assertEquals(emptyList<Float>(), barHeights(emptyList(), maxHeightPx = 100f))
    }

    @Test
    fun `barHeights never goes negative`() {
        assertEquals(listOf(0f, 100f), barHeights(listOf(-5f, 20f), maxHeightPx = 100f))
    }

    @Test
    fun `barHeights of all-zero values returns zeros not NaN`() {
        assertEquals(listOf(0f, 0f), barHeights(listOf(0f, 0f), maxHeightPx = 100f))
    }
}
