// Named for the composable it hosts, not the small data class it also declares.
@file:Suppress("MatchingDeclarationName")

package com.dhruv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/** One slice of an [AllocationStackedBar] — e.g. "Equity" 45% in `--c1`. */
data class AllocationSlice(
    val label: String,
    val value: Float,
    val color: Color,
)

/**
 * DhruvNext §6.2's Home allocation bar: a single rounded stacked bar + a legend row below it,
 * using the [allocationPercentages] pure data-shaping function.
 */
@Composable
fun AllocationStackedBar(
    slices: List<AllocationSlice>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    val percentages = allocationPercentages(slices.map { it.value })

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
        ) {
            slices.forEachIndexed { index, slice ->
                Box(
                    modifier =
                        Modifier
                            .weight(percentages[index].coerceAtLeast(MIN_SLICE_WEIGHT))
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(slice.color),
                )
            }
        }
        Row(modifier = Modifier.padding(top = 10.dp).fillMaxWidth()) {
            slices.forEachIndexed { index, slice ->
                LegendEntry(color = slice.color, label = slice.label, percent = percentages[index], textColor = colors.tx2)
                if (index != slices.lastIndex) {
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }
        }
    }
}

@Composable
private fun LegendEntry(
    color: Color,
    label: String,
    percent: Float,
    textColor: Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(text = " $label ${percent.toInt()}%", color = textColor, fontSize = 11.sp)
    }
}

private const val MIN_SLICE_WEIGHT = 0.001f
