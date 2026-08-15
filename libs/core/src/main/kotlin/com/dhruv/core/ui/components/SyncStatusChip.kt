// Named for the composable it hosts, not the small enum it also declares.
@file:Suppress("MatchingDeclarationName")

package com.dhruv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors

enum class SyncState { SYNCED, SYNCING, PENDING, FAILED }

/**
 * A dot + label chip reporting an entity's sync state — D6 accounts, D7 account detail, G1
 * automation ("N this month" style rows also pair with this for the source's last-sync state).
 */
@Composable
fun SyncStatusChip(
    state: SyncState,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    val dotColor =
        when (state) {
            SyncState.SYNCED -> colors.pos
            SyncState.SYNCING -> colors.acc
            SyncState.PENDING -> colors.warn
            SyncState.FAILED -> colors.neg
        }
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(DhruvNextRadii.pill))
                .background(colors.surf2)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(7.dp).clip(CircleShape).background(dotColor),
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 6.dp),
            color = colors.tx2,
            fontSize = DhruvNextType.meta,
        )
    }
}
