package com.dhruv.finance.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.settings.SettingsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSectionDetailSheet(
    config: SettingsSectionConfig,
    settingsRepository: SettingsRepository,
    onPageToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(config.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            if (config.canDisable) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Show ${config.title} in bottom navigation")
                    Switch(checked = config.enabled, onCheckedChange = onPageToggle)
                }
            }

            if (config.enabled || !config.canDisable) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Visible tools", fontWeight = FontWeight.Bold)

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        config.tools.forEach { tool ->
                            val isEnabledState = settingsRepository.isToolEnabled(tool).collectAsState(initial = true)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            ) {
                                Text(tool, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                Switch(
                                    checked = isEnabledState.value,
                                    onCheckedChange = { settingsRepository.setToolEnabled(tool, it) }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        config.tools.forEach { settingsRepository.setToolEnabled(it, true) }
                    }) {
                        Text("Show all")
                    }
                    TextButton(onClick = {
                        config.tools.forEach { settingsRepository.setToolEnabled(it, false) }
                    }) {
                        Text("Hide all")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
