// Named for the composable it hosts, not the small data class it also declares.
@file:Suppress("MatchingDeclarationName")

package com.dhruv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/** One bar in a [BarChart] — a period label (month/year) and its value. */
data class BarEntry(
    val label: String,
    val value: Float,
)

/**
 * DhruvNext's 6-month/by-year bar chart (§6.4's principal-vs-interest-by-year,
 * §6.5's "Spent in July" 6-month history) — the [highlightIndex] bar (current period) renders in
 * the accent color, the rest in a muted tone.
 */
@Composable
fun BarChart(
    entries: List<BarEntry>,
    modifier: Modifier = Modifier,
    highlightIndex: Int? = null,
    chartHeight: Dp = 120.dp,
) {
    val colors = LocalDhruvNextColors.current
    // maxHeightPx = 1f turns barHeights into a 0..1 fraction usable directly as a RowScope weight.
    val weights = barHeights(entries.map { it.value }, maxHeightPx = 1f)

    Row(
        modifier = modifier.fillMaxWidth().height(chartHeight),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        entries.forEachIndexed { index, entry ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                val tint = if (index == highlightIndex) colors.acc else colors.surf2
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(weights[index].coerceAtLeast(MIN_BAR_WEIGHT))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(tint),
                )
                Text(
                    text = entry.label,
                    color = colors.tx3,
                    fontSize = DhruvNextType.meta,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private const val MIN_BAR_WEIGHT = 0.02f
