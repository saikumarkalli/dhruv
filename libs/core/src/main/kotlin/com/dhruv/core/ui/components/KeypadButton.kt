package com.dhruv.core.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.DhruvNextKeypad
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/**
 * DhruvNext §6.3 keypad button. Three visual roles via [isOperator] × [fillAccent]:
 *
 * - **digit** (both false): `surf` bg + 1dp `line` border, `tx` content.
 * - **fill operator** (`isOperator=true, fillAccent=true`): solid `accSoft` fill, `acc` content.
 * - **outline operator** (`isOperator=true, fillAccent=false`): digit bg but `acc` content.
 * - **primary action** (`solidAccent=true`): solid `acc` fill, `onAcc` content, no border.
 *
 * [contentColorOverride] and [fontWeightOverride] escape-hatch the derived defaults for
 * one-off labels (e.g. bracket toggle's muted `tx2`, C/AC's bolder weight).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeypadButton(
    modifier: Modifier = Modifier,
    text: String? = null,
    icon: ImageVector? = null,
    badgeText: String? = null,
    isOperator: Boolean = false,
    fillAccent: Boolean = false,
    solidAccent: Boolean = false,
    contentColorOverride: Color? = null,
    fontWeightOverride: FontWeight? = null,
    tag: String? = null,
    keyHeight: Dp = 72.dp,
    fontSize: TextUnit = DhruvNextKeypad.digit,
    iconSize: Dp = 26.dp,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val colors = LocalDhruvNextColors.current
    val hapticFeedback = LocalHapticFeedback.current

    val contentColor = contentColorOverride ?: when {
        solidAccent -> colors.onAcc
        isOperator -> colors.acc
        else -> colors.tx
    }
    val fontWeight = fontWeightOverride ?: if (isOperator) FontWeight.SemiBold else FontWeight.Medium

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val rippleProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isPressed) 80 else 200,
            easing = FastOutSlowInEasing,
        ),
        label = "rippleProgress",
    )
    val rippleBaseColor = contentColor
    val keyShape = RoundedCornerShape(DhruvNextRadii.listGroup)

    Box(
        modifier = modifier
            .then(if (keyHeight == Dp.Unspecified) Modifier.fillMaxHeight() else Modifier.height(keyHeight))
            .clip(keyShape)
            .background(
                when {
                    solidAccent -> colors.acc
                    fillAccent -> colors.accSoft
                    else -> colors.surf
                },
            )
            .then(if (fillAccent || solidAccent) Modifier else Modifier.border(1.dp, colors.line, keyShape))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
                onLongClick = onLongClick?.let {
                    {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        it()
                    }
                },
            )
            .drawWithContent {
                drawContent()
                if (rippleProgress > 0f) {
                    val maxRadius = size.minDimension * 0.9f
                    val currentRadius = maxRadius * rippleProgress
                    val alpha = (0.28f * (1f - rippleProgress * 0.5f)).coerceAtLeast(0f)
                    val brush = Brush.radialGradient(
                        colors = listOf(
                            rippleBaseColor.copy(alpha = alpha),
                            rippleBaseColor.copy(alpha = alpha * 0.4f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = currentRadius,
                    )
                    drawCircle(
                        brush = brush,
                        radius = currentRadius,
                        center = Offset(size.width / 2f, size.height / 2f),
                    )
                }
            }
            .testTag(tag ?: if (text != null) "key_btn_$text" else "key_btn_icon"),
        contentAlignment = Alignment.Center,
    ) {
        if (text != null) {
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = contentColor,
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColorOverride ?: when {
                    solidAccent -> colors.onAcc
                    isOperator -> colors.acc
                    else -> colors.tx.copy(alpha = 0.8f)
                },
                modifier = Modifier.size(iconSize),
            )
        }

        if (badgeText != null) {
            Text(
                text = badgeText,
                fontSize = DhruvNextKeypad.caption,
                fontWeight = FontWeight.Bold,
                color = colors.acc,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 24.dp),
            )
        }
    }
}
