package com.dhruv.finance.mocks

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.core.ui.components.DhruvWordmarkVertical
import com.dhruv.core.ui.theme.DhruvGold
import com.dhruv.core.ui.theme.DhruvTheme

@Composable
fun SettingsMock() {
    DhruvTheme {
        Scaffold(
            // Settings is the last tab — no bottom nav per spec
        ) { padding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // DhruvWordmarkVertical header at 60% alpha (brand guide: empty-state / header)
                DhruvWordmarkVertical(
                    appName = "finance",
                    crestTint = DhruvGold,
                    textColor = DhruvGold,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                            .alpha(0.60f),
                )

                SettingsSectionMock(title = "Appearance") {
                    SettingsRowChevron(label = "Theme", value = "System")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SettingsRowChevron(label = "Accent colors", value = "Theme & Accents")
                }

                SettingsSectionMock(title = "Precision") {
                    SettingsRowChevron(label = "Decimal places", value = "4 places")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SettingsRowChevron(label = "Number format", value = "International")
                }

                SettingsSectionMock(title = "Calculator") {
                    SettingsRowValue(label = "Angle mode", value = "DEG")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SettingsRowToggle(label = "Lock history", checked = false)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SettingsRowChevron(label = "Clear history", value = "Permanently wipe")
                }

                SettingsSectionMock(title = "About") {
                    SettingsRowValue(label = "Version", value = "1.0 (build 1)")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SettingsRowValue(label = "App", value = "Dhruv Finance")
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionMock(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = DhruvGold,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsRowChevron(
    label: String,
    value: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontWeight = FontWeight.Medium, fontSize = 15.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsRowValue(
    label: String,
    value: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontWeight = FontWeight.Medium, fontSize = 15.sp)
        Text(value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsRowToggle(
    label: String,
    checked: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontWeight = FontWeight.Medium, fontSize = 15.sp)
        Switch(checked = checked, onCheckedChange = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsMockPreviewLight() = SettingsMock()

@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsMockPreviewDark() = SettingsMock()
