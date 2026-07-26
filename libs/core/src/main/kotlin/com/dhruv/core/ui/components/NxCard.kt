package com.dhruv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/**
 * DhruvNext's one card primitive — flat 20dp-radius surface with the spec's single elevation
 * (approximated as a low-opacity shadow; Compose has no CSS box-shadow equivalent). No
 * glassmorphism (that's [DhruvGlassCard], retired — see ADR-0024).
 */
@Composable
fun NxCard(
    modifier: Modifier = Modifier,
    padding: Dp = DhruvNextSpacing.cardPadding,
    content: @Composable () -> Unit,
) {
    val colors = LocalDhruvNextColors.current
    Column(
        modifier =
            modifier
                .shadow(elevation = 1.dp, shape = RoundedCornerShape(DhruvNextRadii.card), clip = false)
                .clip(RoundedCornerShape(DhruvNextRadii.card))
                .background(colors.surf)
                .padding(padding),
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.tx) {
            content()
        }
    }
}

/** A tonal inset chip/tile surface — [com.dhruv.core.ui.theme.DhruvNextColors.surf2]. */
@Composable
fun NxInsetSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = DhruvNextRadii.innerTile,
    content: @Composable () -> Unit,
) {
    val colors = LocalDhruvNextColors.current
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(colors.surf2),
    ) {
        content()
    }
}
