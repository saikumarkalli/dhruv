package com.dhruv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors

enum class NxButtonVariant { Primary, Soft, Outline, Ghost, Destructive }

@Composable
fun NxButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: NxButtonVariant = NxButtonVariant.Primary,
    enabled: Boolean = true,
) {
    val colors = LocalDhruvNextColors.current
    val shape = RoundedCornerShape(DhruvNextRadii.innerTile)
    val background =
        when (variant) {
            NxButtonVariant.Primary -> colors.acc
            NxButtonVariant.Soft -> colors.accSoft
            NxButtonVariant.Outline -> Color.Transparent
            NxButtonVariant.Ghost -> Color.Transparent
            NxButtonVariant.Destructive -> colors.neg
        }
    val textColor =
        when (variant) {
            NxButtonVariant.Primary -> colors.onAcc
            NxButtonVariant.Soft -> colors.acc
            NxButtonVariant.Outline -> colors.acc
            NxButtonVariant.Ghost -> colors.tx2
            NxButtonVariant.Destructive -> Color.White
        }
    Row(
        modifier =
            modifier
                .alpha(if (enabled) 1f else 0.7f)
                .clip(shape)
                .background(background)
                .let {
                    when (variant) {
                        NxButtonVariant.Outline -> it.border(1.5.dp, colors.accLine, shape)
                        NxButtonVariant.Ghost -> it.border(1.dp, colors.line, shape)
                        else -> it
                    }
                }
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = DhruvNextType.cardTitle,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun NxIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = LocalDhruvNextColors.current.tx2,
) {
    val colors = LocalDhruvNextColors.current
    val shape = RoundedCornerShape(DhruvNextRadii.innerTile)
    Box(
        modifier =
            modifier
                .size(44.dp)
                .clip(shape)
                .background(colors.surf2)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
        )
    }
}
