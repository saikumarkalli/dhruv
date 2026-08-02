package com.dhruv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.LocalDhruvNextColors

@Composable
fun NxCard(
    modifier: Modifier = Modifier,
    padding: Dp = DhruvNextSpacing.cardPadding,
    content: @Composable () -> Unit,
) {
    val colors = LocalDhruvNextColors.current
    val shape = RoundedCornerShape(DhruvNextRadii.card)
    Column(
        modifier =
            modifier
                .clip(shape)
                .border(1.dp, colors.line, shape)
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
