package com.dhruv.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.LocalDhruvNextColors

private const val SHIMMER_MIN_ALPHA = 0.35f
private const val SHIMMER_MAX_ALPHA = 0.85f
private const val SHIMMER_DURATION_MS = 1100

/**
 * A shimmering placeholder block for a loading row/card — every network-backed screen's loading
 * state (NFR-4). Sizes itself to [width]/[height]; defaults are one text-line-sized block.
 */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
    cornerRadius: Dp = DhruvNextRadii.innerTile,
) {
    val colors = LocalDhruvNextColors.current
    val transition = rememberInfiniteTransition(label = "skeleton-shimmer")
    val alpha by transition.animateFloat(
        initialValue = SHIMMER_MIN_ALPHA,
        targetValue = SHIMMER_MAX_ALPHA,
        animationSpec =
            infiniteRepeatable(
                animation = tween(SHIMMER_DURATION_MS, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "skeleton-alpha",
    )
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(cornerRadius))
                .background(colors.surf2.copy(alpha = alpha)),
    )
}
