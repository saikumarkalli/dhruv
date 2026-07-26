package com.dhruv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.core.ui.theme.LocalDhruvNextColors

private const val DISABLED_ALPHA = 0.55f

/**
 * DhruvNext §6.2's Home quick-action tile — an equal square in a horizontally-scrolling row.
 * [enabled] = false renders the "coming soon" 55%-opacity state (e.g. Invest on Home).
 */
@Composable
fun QuickActionTile(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    val colors = LocalDhruvNextColors.current
    Column(
        modifier =
            modifier
                .size(width = 68.dp, height = 72.dp)
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .let { if (enabled) it.clickable(onClick = onClick) else it },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surf2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = colors.acc)
        }
        Text(
            text = label,
            modifier = Modifier.padding(top = 6.dp),
            color = colors.tx2,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
