package com.dhruv.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/**
 * A filled pie (design system B3) — [DonutChart] without the hole, for the screens the design
 * draws as a solid wedge chart rather than a ring (e.g. category-mix breakdowns that don't carry
 * a center total). Shares [DonutSegment] and [sweepAngles] with [DonutChart]/[AmortisationDonut].
 */
@Composable
fun PieChart(
    segments: List<DonutSegment>,
    modifier: Modifier = Modifier.size(160.dp),
) {
    val colors = LocalDhruvNextColors.current
    val sweeps = sweepAngles(segments.map { it.value })
    Canvas(modifier = modifier) {
        val topLeft = Offset.Zero
        val fullSize = Size(size.width, size.height)
        if (segments.isEmpty() || sweeps.all { it <= 0f }) {
            drawArc(
                color = colors.surf2,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = true,
                topLeft = topLeft,
                size = fullSize,
            )
        } else {
            var start = -90f
            segments.forEachIndexed { index, segment ->
                val sweep = sweeps[index]
                if (sweep > 0f) {
                    drawArc(
                        color = segment.color,
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = true,
                        topLeft = topLeft,
                        size = fullSize,
                    )
                    start += sweep
                }
            }
        }
    }
}
