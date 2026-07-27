// Named for the composable it hosts, not the small enum it also declares.
@file:Suppress("MatchingDeclarationName")

package com.dhruv.core.ui.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.dhruv.core.format.Paise
import com.dhruv.core.ui.theme.DhruvNextType

/** Text weight/size preset for [MoneyText] — DhruvNext §4's hero/card-title/body type scale. */
enum class MoneyTextVariant {
    /** Hero amount (net worth, EMI, corpus, GST total): [DhruvNextType.hero]/700, tight tracking. */
    Hero,

    /** Card-row amount: [DhruvNextType.cardTitle]/700. */
    Row,

    /** Inline amount within a sentence or meta line: [DhruvNextType.body]/400. */
    Inline,
}

/**
 * The single money renderer for the DhruvNext component library — always tabular-numeral, Indian
 * grouping, via [Paise]. Never hand-format a paise amount with `Text(...)` directly.
 */
@Composable
fun MoneyText(
    paise: Long,
    modifier: Modifier = Modifier,
    variant: MoneyTextVariant = MoneyTextVariant.Row,
    compact: Boolean = false,
    color: Color = LocalContentColor.current,
) {
    val text = if (compact) Paise.formatCompact(paise) else Paise.format(paise)
    val (fontSize, weight, tracking) =
        when (variant) {
            MoneyTextVariant.Hero -> Triple(DhruvNextType.hero, FontWeight.Bold, (-1.5).sp)
            MoneyTextVariant.Row -> Triple(DhruvNextType.cardTitle, FontWeight.Bold, 0.sp)
            MoneyTextVariant.Inline -> Triple(DhruvNextType.body, FontWeight.Normal, 0.sp)
        }
    Text(
        text = text,
        modifier = modifier,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style =
            TextStyle(
                color = color,
                fontSize = fontSize,
                fontWeight = weight,
                letterSpacing = tracking,
                fontFeatureSettings = "tnum",
            ),
    )
}
