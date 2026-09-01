package com.dhruv.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * A [ProgressRing] plus a fixed pace marker (design system B3) — budgets/goals where "on pace"
 * means the actual [progress] arc should sit at or past [paceMarkerProgress] (e.g. "18 of 30 days
 * elapsed" on a monthly budget ring). The marker is a small dot on the ring's own track, not a
 * second arc, so it reads as a target rather than a second value.
 */
@Composable
fun PaceRing(
    progress: Float,
    paceMarkerProgress: Float,
    modifier: Modifier = Modifier.size(88.dp),
    strokeWidth: Dp = 10.dp,
    centerContent: @Composable () -> Unit = {},
) {
    val colors = LocalDhruvNextColors.current
    val clampedProgress = progress.coerceIn(0f, 1f)
    val clampedMarker = paceMarkerProgress.coerceIn(0f, 1f)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            drawArc(color = colors.surf2, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = stroke)
            drawArc(
                color = colors.acc,
                startAngle = -90f,
                sweepAngle = clampedProgress * 360f,
                useCenter = false,
                style = stroke,
            )

            val markerAngleDeg = -90f + clampedMarker * 360f
            val markerAngleRad = Math.toRadians(markerAngleDeg.toDouble())
            val radius = (size.minDimension - strokeWidth.toPx()) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val markerCenter =
                Offset(
                    x = center.x + radius * cos(markerAngleRad).toFloat(),
                    y = center.y + radius * sin(markerAngleRad).toFloat(),
                )
            drawCircle(color = colors.tx, radius = strokeWidth.toPx() / 2.4f, center = markerCenter)
            drawCircle(color = colors.surf, radius = strokeWidth.toPx() / 4.8f, center = markerCenter)
        }
        centerContent()
    }
}
