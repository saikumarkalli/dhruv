package com.dhruv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors

private val LabelSize = 12.sp
private val CornerRadius = 13.dp
private val BorderWidth = 1.5.dp
private val HorizontalPadding = 14.dp
private val VerticalPadding = 11.dp
private val LabelBottomSpacing = 6.dp

/**
 * DhruvNext's dropdown field (design system B6, e.g. "Reducing balance ▾") — visually [NxTextField]'s
 * twin, but the value is never typed: tapping the field is the caller's cue to show a picker
 * (typically [SelectionSheet]). This composable owns no popup/menu state itself, same division of
 * labor as [NxTextField] owning no validation logic.
 */
@Composable
fun NxSelect(
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    errorMessage: String? = null,
) {
    val colors = LocalDhruvNextColors.current
    val shape = RoundedCornerShape(CornerRadius)
    val borderColor = if (errorMessage != null) colors.neg else colors.line

    Column(modifier = modifier.alpha(if (enabled) 1f else 0.45f)) {
        if (label != null) {
            Text(
                text = label,
                color = colors.tx2,
                fontSize = LabelSize,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.padding(bottom = LabelBottomSpacing))
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(colors.surf)
                    .border(BorderWidth, borderColor, shape)
                    .clickable(enabled = enabled, onClick = onClick)
                    .padding(horizontal = HorizontalPadding, vertical = VerticalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Text(
                    text = value ?: placeholder.orEmpty(),
                    color = if (value != null) colors.tx else colors.tx3,
                    fontSize = DhruvNextType.cardTitle,
                )
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = colors.tx3,
            )
        }

        if (errorMessage != null) {
            Spacer(Modifier.padding(top = LabelBottomSpacing))
            Text(
                text = errorMessage,
                color = colors.neg,
                fontSize = LabelSize,
            )
        }
    }
}
