package com.dhruv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.core.ui.theme.LocalDhruvNextColors

private const val BADGE_CAP = 99

/** Caps a raw count at "99+" — DhruvNext §4: small tonal count chip, max "99+". */
fun formatBadgeCount(count: Int): String = if (count > BADGE_CAP) "$BADGE_CAP+" else "$count"

/** A small tonal count chip — unread notifications, filter-result counts. */
@Composable
fun CountBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    if (count <= 0) return
    val colors = LocalDhruvNextColors.current
    Box(
        modifier =
            modifier
                .clip(CircleShape)
                .background(colors.accSoft)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = formatBadgeCount(count), color = colors.acc, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
