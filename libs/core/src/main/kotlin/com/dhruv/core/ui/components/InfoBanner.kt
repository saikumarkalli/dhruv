package com.dhruv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/**
 * A persistent inline informational strip (design batch B7) — the third of the design's three
 * feedback shapes alongside the snackbar ([UndoSnackbarHost]) and [OfflineBanner]: non-dismissible
 * context that stays on screen, e.g. D6's "automatic balance refresh arrives with account linking"
 * footnote.
 */
@Composable
fun InfoBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DhruvNextRadii.innerTile))
                .background(colors.surf2)
                .padding(DhruvNextSpacing.interCardGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = colors.tx3)
        Text(
            text = message,
            color = colors.tx2,
            fontSize = DhruvNextType.meta,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}
