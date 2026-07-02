package com.dhruv.core.ui.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glassmorphism card for the Dhruv design system.
 * On API 31+ the background layer is blurred; on older APIs a plain semi-transparent
 * surface is used — no external blur library required.
 */
@Composable
fun DhruvGlassCard(
    modifier: Modifier = Modifier,
    backgroundAlpha: Float = 0.12f,
    cornerRadius: Dp = 16.dp,
    blurRadiusPx: Float = 20f,
    borderAlpha: Float = 0.20f,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val surface = MaterialTheme.colorScheme.surface

    Box(modifier = modifier.clip(shape)) {
        // Background layer — blurred on API 31+, plain semi-transparent below
        Spacer(
            modifier =
                Modifier
                    .matchParentSize()
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.graphicsLayer {
                                renderEffect = BlurEffect(blurRadiusPx, blurRadiusPx, TileMode.Clamp)
                            }
                        } else {
                            Modifier
                        },
                    ).background(color = surface.copy(alpha = backgroundAlpha)),
        )
        // Glass border shimmer
        Spacer(
            modifier =
                Modifier
                    .matchParentSize()
                    .border(
                        border = BorderStroke(width = 1.dp, color = Color.White.copy(alpha = borderAlpha)),
                        shape = shape,
                    ),
        )
        // Content sits on top, unblurred
        Box(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}
