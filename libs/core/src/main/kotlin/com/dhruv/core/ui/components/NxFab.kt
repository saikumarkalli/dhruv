package com.dhruv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors

private val FAB_MINI_SIZE = 44.dp

/**
 * Round icon-only FAB — the quick-add trigger on list roots (D1 ledger, C2/C6/D6/D8/E4/E7's
 * "add" actions). For the labelled variant with a text alongside the icon (D1's "+ Quick add"
 * treatment on wider layouts) use [NxExtendedFab].
 */
@Composable
fun NxFab(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val colors = LocalDhruvNextColors.current
    Box(
        modifier =
            modifier
                .size(FAB_MINI_SIZE)
                .clip(CircleShape)
                .background(colors.acc)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = colors.onAcc)
    }
}

/** Icon + label FAB — used where the action needs a name, not just an icon (design's "+ New"). */
@Composable
fun NxExtendedFab(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    val shape = RoundedCornerShape(DhruvNextRadii.pill)
    Row(
        modifier =
            modifier
                .height(FAB_MINI_SIZE)
                .clip(shape)
                .background(colors.acc)
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = colors.onAcc)
        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp),
            color = colors.onAcc,
            fontSize = DhruvNextType.cardTitle,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
