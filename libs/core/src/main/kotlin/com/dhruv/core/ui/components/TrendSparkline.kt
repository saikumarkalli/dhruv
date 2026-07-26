package com.dhruv.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/** DhruvNext §6.2's Home net-worth trend — an SVG area-line sparkline, drawn on [Canvas]. */
@Composable
fun TrendSparkline(
    values: List<Float>,
    modifier: Modifier = Modifier.fillMaxWidth().height(56.dp),
) {
    val colors = LocalDhruvNextColors.current
    if (values.size < 2) return

    Canvas(modifier = modifier) {
        val points = sparklinePoints(values, width = size.width, height = size.height)

        val linePath =
            Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
        val areaPath =
            Path().apply {
                addPath(linePath)
                lineTo(points.last().x, size.height)
                lineTo(points.first().x, size.height)
                close()
            }

        drawPath(areaPath, color = colors.acc.copy(alpha = AREA_FILL_ALPHA), style = Fill)
        drawPath(linePath, color = colors.acc, style = Stroke(width = LINE_WIDTH_PX))
    }
}

private const val AREA_FILL_ALPHA = 0.14f
private const val LINE_WIDTH_PX = 4f
