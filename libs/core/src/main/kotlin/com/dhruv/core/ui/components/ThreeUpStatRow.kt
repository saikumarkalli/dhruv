// Named for the composable it hosts, not the small data class it also declares.
@file:Suppress("MatchingDeclarationName")

package com.dhruv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/** One entry in a [ThreeUpStatRow]. */
data class StatItem(
    val label: String,
    val value: String,
    val highlighted: Boolean = false,
)

/**
 * A 3-up stat row — DhruvNext §6.2's "July cash flow" (In/Out/Saved), §6.4's Total payable/
 * Interest share/Ends-date, etc. The middle/highlighted tile gets an accent-soft background.
 */
@Composable
fun ThreeUpStatRow(
    items: List<StatItem>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(DhruvNextRadii.innerTile))
                        .let { if (item.highlighted) it.background(colors.accSoft) else it.background(colors.surf2) }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = item.label, color = colors.tx2, fontSize = 11.sp)
                Text(
                    text = item.value,
                    color = if (item.highlighted) colors.acc else colors.tx,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
