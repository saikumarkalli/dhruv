package com.dhruv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
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

/**
 * A delta pill — up/down amount or percent, e.g. "+4 points this month" or "12%". Never color
 * alone (`platform/DESIGN-SYSTEM.md` §1 rule): always paired with a ▲/▼ glyph.
 */
@Composable
fun StatDeltaChip(
    text: String,
    isPositive: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    val tint = if (isPositive) colors.pos else colors.neg
    val softBg = if (isPositive) colors.posSoft else colors.negSoft
    val glyph = if (isPositive) "▲" else "▼"
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(DhruvNextRadii.pill))
                .background(softBg)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "$glyph $text", color = tint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
