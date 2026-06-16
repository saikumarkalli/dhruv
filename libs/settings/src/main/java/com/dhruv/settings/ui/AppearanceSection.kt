package com.dhruv.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.AppTheme
import com.dhruv.core.ui.theme.DhruvFont
import com.dhruv.settings.AppSettings

/**
 * Reusable Appearance settings section — theme, accent colour, font.
 *
 * Intentionally has NO dependency on any finance-specific code. All actions are callback-driven.
 *
 * @param settings Current [AppSettings] snapshot.
 * @param onThemeChanged Called when the user selects a new [AppTheme].
 * @param onAccentColorHexChanged Called with a validated "#RRGGBB" string or null if the input is
 *   invalid / empty (caller decides whether to reset to default).
 * @param onFontChanged Called when the user selects a new [DhruvFont].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSection(
    settings: AppSettings,
    onThemeChanged: (AppTheme) -> Unit,
    onAccentColorHexChanged: (String) -> Unit,
    onFontChanged: (DhruvFont) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Theme", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)

        val themes = listOf(
            AppTheme.SYSTEM to "System",
            AppTheme.LIGHT to "Light",
            AppTheme.DARK to "Dark"
        )
        val selectedIndex = themes.indexOfFirst { it.first == settings.theme }.coerceAtLeast(0)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            themes.forEachIndexed { index, (theme, label) ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = themes.size),
                    onClick = { onThemeChanged(theme) },
                    selected = index == selectedIndex
                ) {
                    Text(label)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text("Accent colour (hex)", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)

        var hexInput by remember(settings.accentColorHex) { mutableStateOf(settings.accentColorHex) }
        val isHexValid = remember(hexInput) { hexInput.matches(Regex("^#[0-9A-Fa-f]{6}$")) }

        OutlinedTextField(
            value = hexInput,
            onValueChange = { raw ->
                hexInput = raw
                if (raw.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                    onAccentColorHexChanged(raw)
                }
            },
            label = { Text("#RRGGBB") },
            placeholder = { Text("#D4AF37") },
            isError = hexInput.isNotEmpty() && !isHexValid,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )
        if (hexInput.isNotEmpty() && !isHexValid) {
            Text(
                text = "Enter a valid hex colour, e.g. #D4AF37",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text("Font", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)

        var fontExpanded by remember { mutableStateOf(false) }
        val fontLabels = mapOf(
            DhruvFont.DEFAULT to "Default",
            DhruvFont.ROUNDED to "Rounded",
            DhruvFont.MONO to "Monospace"
        )

        ExposedDropdownMenuBox(
            expanded = fontExpanded,
            onExpandedChange = { fontExpanded = it }
        ) {
            OutlinedTextField(
                value = fontLabels[settings.fontFamily] ?: "Default",
                onValueChange = {},
                readOnly = true,
                label = { Text("Font family") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fontExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = fontExpanded,
                onDismissRequest = { fontExpanded = false }
            ) {
                fontLabels.forEach { (font, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onFontChanged(font)
                            fontExpanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}
