// Named for the composable it hosts, not the small data class it also declares (BarChart.kt
// precedent).
@file:Suppress("MatchingDeclarationName")

package com.dhruv.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/**
 * One slice of a [DonutChart]/[PieChart]/[RankedLegend] — [displayValue] is pre-formatted by the
 * caller (e.g. `MoneyText`'s helpers), same contract as [MoneyText] itself: this component never
 * formats money.
 */
data class DonutSegment(
    val label: String,
    val value: Float,
    val displayValue: String,
    val color: Color,
)

/**
 * DhruvNext's ranked-sector donut (design system B3) — C1's net-worth-by-sector visual. A hollow
 * ring of [segments] in descending-share order is the caller's responsibility (this draws
 * whatever order it's given); [centerContent] is the caller's net/total slot, same pattern as
 * [CountdownRing].
 */
@Composable
fun DonutChart(
    segments: List<DonutSegment>,
    modifier: Modifier = Modifier.size(160.dp),
    strokeWidth: Dp = 22.dp,
    centerContent: @Composable () -> Unit = {},
) {
    val colors = LocalDhruvNextColors.current
    val sweeps = sweepAngles(segments.map { it.value })
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidth.toPx())
            if (segments.isEmpty() || sweeps.all { it <= 0f }) {
                drawArc(color = colors.surf2, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = stroke)
            } else {
                var start = -90f
                segments.forEachIndexed { index, segment ->
                    val sweep = sweeps[index]
                    if (sweep > 0f) {
                        drawArc(color = segment.color, startAngle = start, sweepAngle = sweep, useCenter = false, style = stroke)
                        start += sweep
                    }
                }
            }
        }
        centerContent()
    }
}

/**
 * A [DonutChart]'s companion list — one row per segment: color swatch, label, share of the total
 * (already computed by the caller, matching [DonutSegment.value]'s own weight) and
 * [DonutSegment.displayValue]. Ordering is the caller's — pass segments already ranked.
 */
@Composable
fun RankedLegend(
    segments: List<DonutSegment>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    val shares = sweepAngles(segments.map { it.value }).map { it / 360f * 100f }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        segments.forEachIndexed { index, segment ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(segment.color),
                )
                Text(
                    text = segment.label,
                    color = colors.tx,
                    fontSize = DhruvNextType.body,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 10.dp).weight(1f),
                )
                Text(
                    text = segment.displayValue,
                    color = colors.tx,
                    fontSize = DhruvNextType.body,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "%.0f%%".format(shares.getOrElse(index) { 0f }),
                    color = colors.tx3,
                    fontSize = DhruvNextType.meta,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
