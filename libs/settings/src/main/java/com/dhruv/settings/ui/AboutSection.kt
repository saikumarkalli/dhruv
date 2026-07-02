package com.dhruv.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable About section.
 *
 * @param versionName The app version name from BuildConfig.VERSION_NAME (passed by the app to
 *   avoid a direct BuildConfig dependency in the library).
 * @param platformVersion A human-readable platform version string (e.g. "Phase 3").
 *   Raw-resource bundling for a feature-flag resolver is deferred to Step 3 — for now the app
 *   passes a constant.
 */
@Composable
fun AboutSection(
    versionName: String,
    platformVersion: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Dhruv Finance",
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Version $versionName",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
        if (platformVersion.isNotBlank()) {
            Text(
                text = "Platform: $platformVersion",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
        }
    }
}
