package com.dhruv.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.core.ui.theme.LocalDhruvNextColors

private fun sweepAngle(progress: Float): Float = progress.coerceIn(0f, 1f) * 360f

/** DhruvNext §5's stateless progress ring — a 4dp-stroke Canvas arc, no center content. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier.size(24.dp),
    strokeWidth: Dp = 4.dp,
) {
    val colors = LocalDhruvNextColors.current
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        drawArc(color = colors.surf2, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = stroke)
        drawArc(color = colors.acc, startAngle = -90f, sweepAngle = sweepAngle(progress), useCenter = false, style = stroke)
    }
}

/** DhruvNext §6.2's Home financial-health ring — 88dp conic donut with "score / of 100" centered. */
@Composable
fun FinancialHealthRing(
    score: Int,
    modifier: Modifier = Modifier.size(88.dp),
    maxScore: Int = 100,
) {
    val colors = LocalDhruvNextColors.current
    val progress = (score.toFloat() / maxScore.toFloat()).coerceIn(0f, 1f)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(88.dp)) {
            val stroke = Stroke(width = HEALTH_RING_STROKE_PX, cap = StrokeCap.Round)
            drawArc(color = colors.surf2, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = stroke)
            drawArc(color = colors.acc, startAngle = -90f, sweepAngle = sweepAngle(progress), useCenter = false, style = stroke)
        }
        Text(
            text = "$score",
            color = colors.tx,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

/** DhruvNext §6.6's timer — a 250dp conic countdown ring; center content (time text) is the caller's. */
@Composable
fun CountdownRing(
    progress: Float,
    modifier: Modifier = Modifier.size(250.dp),
    centerContent: @Composable () -> Unit = {},
) {
    val colors = LocalDhruvNextColors.current
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(250.dp)) {
            val stroke = Stroke(width = COUNTDOWN_RING_STROKE_PX, cap = StrokeCap.Round)
            drawArc(color = colors.surf2, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = stroke)
            drawArc(color = colors.acc, startAngle = -90f, sweepAngle = sweepAngle(progress), useCenter = false, style = stroke)
        }
        centerContent()
    }
}

private const val HEALTH_RING_STROKE_PX = 10f
private const val COUNTDOWN_RING_STROKE_PX = 14f
