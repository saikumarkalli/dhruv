package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.core.ui.theme.ColorOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAppearanceSheet(
    uiState: SettingsUiState,
    onThemeChanged: (String) -> Unit,
    onColorChanged: (String, String) -> Unit, // section, colorId
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Appearance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            
            // Theme Segmented Control
            val themes = listOf("system" to "System", "always_light" to "Light", "always_dark" to "Dark")
            val selectedIndex = themes.indexOfFirst { it.first == uiState.darkModePreference }.takeIf { it >= 0 } ?: 0
            
            Text("Theme", fontWeight = FontWeight.Bold)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                themes.forEachIndexed { index, pair ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = themes.size),
                        onClick = { onThemeChanged(pair.first) },
                        selected = index == selectedIndex
                    ) {
                        Text(pair.second)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Section colors", fontWeight = FontWeight.Bold)

            val targets = listOf(
                AccentTarget("calculator", "Calculator", uiState.calculatorColor),
                AccentTarget("converter", "Converter", uiState.converterColor),
                AccentTarget("date", "Date & Time", uiState.dateColor),
                AccentTarget("finance", "Finance", uiState.financeColor),
                AccentTarget("time", "Time Tools", uiState.timeColor)
            )

            targets.forEach { target ->
                ColorPickerRow(
                    label = target.label,
                    selectedColorId = target.selectedColorId,
                    onColorSelected = { colorId -> onColorChanged(target.id, colorId) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ColorPickerRow(
    label: String,
    selectedColorId: String,
    onColorSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(ColorOptions) { opt ->
                    val isSelected = opt.id == selectedColorId
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(opt.darkPrimary)
                            .clickable { onColorSelected(opt.id) }
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        }
    }
}
