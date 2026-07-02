package com.dhruv.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable Sync settings section — stub for Phase 2 sync feature.
 *
 * Toggle is always greyed out with a "Coming soon" label until sync is implemented (Phase 2).
 *
 * No finance dependencies.
 */
@Composable
fun SyncSection(
    syncEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Cloud sync",
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Coming soon",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Switch(
                checked = syncEnabled,
                onCheckedChange = null, // disabled — stub
                enabled = false,
            )
        }
    }
}
