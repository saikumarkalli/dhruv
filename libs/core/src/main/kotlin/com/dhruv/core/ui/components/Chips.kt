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
import androidx.compose.ui.unit.sp
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
        Text(text = label, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

/** A fully-rounded pill — mode selectors, CTAs, preset amount/tip chips. */
@Composable
fun Pill(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    filled: Boolean = false,
    leadingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = LocalDhruvNextColors.current
    val background =
        when {
            filled -> colors.acc
            selected -> colors.accSoft
            else -> colors.surf2
        }
    val textColor =
        when {
            filled -> colors.onAcc
            selected -> colors.acc
            else -> colors.tx2
        }
    val border = if (selected && !filled) BorderStroke(1.dp, colors.accLine) else null

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
        Text(text = label, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
