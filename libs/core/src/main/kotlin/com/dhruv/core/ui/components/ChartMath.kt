package com.dhruv.core.ui.components

import androidx.compose.ui.geometry.Offset

/** Pure data-shaping backing [TrendSparkline] — maps values onto a `width`×`height` canvas. */
fun sparklinePoints(
    values: List<Float>,
    width: Float,
    height: Float,
): List<Offset> =
    when {
        values.isEmpty() -> emptyList()
        values.size == 1 -> listOf(Offset(width / 2f, height / 2f))
        else -> {
            val min = values.min()
            val range = values.max() - min
            values.mapIndexed { index, value ->
                val x = width * index / (values.size - 1)
                val normalized = if (range == 0f) 0.5f else (value - min) / range
                Offset(x, height - normalized * height)
            }
        }
    }

/** Pure data-shaping backing [AllocationStackedBar] — each value's share of the total, as 0-100. */
fun allocationPercentages(values: List<Float>): List<Float> {
    val sum = values.sum()
    if (sum == 0f) return values.map { 0f }
    return values.map { it / sum * 100f }
}

/** Pure data-shaping backing [BarChart] — each value's bar height in px, relative to the tallest. */
fun barHeights(
    values: List<Float>,
    maxHeightPx: Float,
): List<Float> {
    if (values.isEmpty()) return emptyList()
    val max = values.maxOrNull()?.coerceAtLeast(0f) ?: 0f
    return if (max <= 0f) {
        values.map { 0f }
    } else {
        values.map { (it / max * maxHeightPx).coerceAtLeast(0f) }
    }
}
