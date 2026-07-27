package com.dhruv.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/** A small rounded-rectangle chip — filter/tag rows, category chips. */
@Composable
fun Chip(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = LocalDhruvNextColors.current
    val background = if (selected) colors.accSoft else colors.surf2
    val textColor = if (selected) colors.acc else colors.tx2
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(10.dp))
                .background(background)
                .let { if (onClick != null) it.clickable(onClick = onClick) else it }
                .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = textColor, fontSize = DhruvNextType.body, fontWeight = FontWeight.Medium)
    }
}

/**
 * A fully-rounded pill — mode selectors, CTAs, preset amount/tip chips.
 *
 * [strong] switches the selected/unselected styling from the default accent-soft look (preset
 * amount/tip chips — `selected` = `accSoft` bg + `acc` text) to the segment/strong look DhruvNext
 * uses for mode/category selectors (`selected` = solid `tx` bg + `bg` text + 700 weight;
 * unselected = `surf` bg + a 1dp `line` border + 600 weight, instead of the soft variant's
 * borderless `surf2` + 500 weight). [filled] (the solid-accent CTA look, e.g. [AskPill]) takes
 * precedence over both if combined.
 */
@Composable
fun Pill(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    filled: Boolean = false,
    strong: Boolean = false,
    leadingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = LocalDhruvNextColors.current
    val background =
        when {
            filled -> colors.acc
            strong && selected -> colors.tx
            strong -> colors.surf
            selected -> colors.accSoft
            else -> colors.surf2
        }
    val textColor =
        when {
            filled -> colors.onAcc
            strong && selected -> colors.bg
            strong -> colors.tx2
            selected -> colors.acc
            else -> colors.tx2
        }
    val fontWeight =
        when {
            filled -> FontWeight.Medium
            strong && selected -> FontWeight.Bold
            strong -> FontWeight.SemiBold
            else -> FontWeight.Medium
        }
    val border =
        when {
            strong && !selected -> BorderStroke(1.dp, colors.line)
            selected && !filled && !strong -> BorderStroke(1.dp, colors.accLine)
            else -> null
        }

    Row(
        modifier =
            modifier
                .clip(CircleShape)
                .background(background)
                .let { if (border != null) it.border(border, CircleShape) else it }
                .let { if (onClick != null) it.clickable(onClick = onClick) else it }
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.padding(end = 6.dp),
            )
        }
        Text(text = label, color = textColor, fontSize = DhruvNextType.body, fontWeight = fontWeight)
    }
}
