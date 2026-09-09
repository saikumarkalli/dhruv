package com.dhruv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/**
 * A pending/proposed row (D9-review) — dashed border marks it as "not yet real": nothing lands in
 * any total until [onAccept] is tapped (FR-029). Accept/Dismiss are inline actions, not a swipe
 * gesture, so the choice is always visible and never accidental.
 */
@Composable
fun SuggestedRow(
    title: String,
    subtitle: String,
    amountText: String,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    val radius = DhruvNextRadii.card
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(radius))
                .background(colors.surf2)
                .dashedOutline(colors.lineStrong, radius)
                .padding(DhruvNextSpacing.interCardGap),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = colors.tx, fontSize = DhruvNextType.cardTitle, fontWeight = FontWeight.Medium)
                Text(text = subtitle, color = colors.tx2, fontSize = DhruvNextType.meta)
            }
            Text(text = amountText, color = colors.tx, fontSize = DhruvNextType.cardTitle, fontWeight = FontWeight.Bold)
        }
        Row(modifier = Modifier.padding(top = 8.dp)) {
            NxButton(text = "Accept", onClick = onAccept, size = NxButtonSize.Small, variant = NxButtonVariant.Soft)
            Spacer(modifier = Modifier.width(8.dp))
            NxButton(text = "Dismiss", onClick = onDismiss, size = NxButtonSize.Small, variant = NxButtonVariant.Ghost)
        }
    }
}

/** A dashed rounded-rect outline — Compose's built-in `.border()` cannot express a dash pattern. */
private fun Modifier.dashedOutline(
    color: Color,
    cornerRadius: androidx.compose.ui.unit.Dp,
): Modifier =
    drawBehind {
        val strokeWidthPx = 1.5.dp.toPx()
        drawRoundRect(
            color = color,
            style =
                Stroke(
                    width = strokeWidthPx,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx()), 0f),
                ),
            cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
        )
    }
