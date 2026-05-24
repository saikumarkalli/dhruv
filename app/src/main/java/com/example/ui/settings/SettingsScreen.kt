package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SettingsRepository
import com.example.ui.calculator.CalculatorViewModel
import com.example.ui.theme.ColorOptions
import com.example.ui.theme.appGradientBackground

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    calculatorViewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val isDegree by settingsRepository.isDegree.collectAsState()
    val darkModePreference by settingsRepository.darkModePreference.collectAsState()
    val decimalPrecision by settingsRepository.decimalPrecision.collectAsState()
    val isHistoryLocked by settingsRepository.isHistoryLocked.collectAsState()
    val historyPinCode by settingsRepository.historyPinCode.collectAsState()

    val calculatorColor by settingsRepository.calculatorColor.collectAsState()
    val converterColor by settingsRepository.converterColor.collectAsState()
    val dateColor by settingsRepository.dateColor.collectAsState()
    val financeColor by settingsRepository.financeColor.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showPrecisionDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }

    var activeColorTargetSetting by remember { mutableStateOf<String?>(null) }
    var showColorPickerDialog by remember { mutableStateOf(false) }

    val activeCalColorName = ColorOptions.firstOrNull { it.id == calculatorColor }?.name ?: "Neon Cyan"
    val activeConvColorName = ColorOptions.firstOrNull { it.id == converterColor }?.name ?: "Nebula Purple"
    val activeDateColorName = ColorOptions.firstOrNull { it.id == dateColor }?.name ?: "Aurora Coral"
    val activeFinanceColorName = ColorOptions.firstOrNull { it.id == financeColor }?.name ?: "Cosmic Amber"

    Column(
        modifier = modifier
            .fillMaxSize()
            .appGradientBackground()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings Icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "App Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Behavior Options
        SettingsCategory(title = "Calculator Behavior") {
            SettingsClickableItem(
                icon = Icons.Default.Dialpad,
                title = "Rounding Decimals",
                valueDisplay = "$decimalPrecision decimal digits",
                onClick = { showPrecisionDialog = true },
                tag = "settings_precision_item"
            )
        }



        // Section Theme Colors
        SettingsCategory(title = "Specific Section Themes") {
            SettingsClickableItem(
                icon = Icons.Default.Palette,
                title = "Calculator Page Accent",
                valueDisplay = activeCalColorName,
                onClick = {
                    activeColorTargetSetting = "calculator"
                    showColorPickerDialog = true
                },
                tag = "settings_cal_color_item"
            )
            Divider(modifier = Modifier.padding(vertical = 6.dp))
            SettingsClickableItem(
                icon = Icons.Default.Palette,
                title = "Converter Page Accent",
                valueDisplay = activeConvColorName,
                onClick = {
                    activeColorTargetSetting = "converter"
                    showColorPickerDialog = true
                },
                tag = "settings_conv_color_item"
            )
            Divider(modifier = Modifier.padding(vertical = 6.dp))
            SettingsClickableItem(
                icon = Icons.Default.Palette,
                title = "Date & Time Page Accent",
                valueDisplay = activeDateColorName,
                onClick = {
                    activeColorTargetSetting = "date"
                    showColorPickerDialog = true
                },
                tag = "settings_date_color_item"
            )
            Divider(modifier = Modifier.padding(vertical = 6.dp))
            SettingsClickableItem(
                icon = Icons.Default.Palette,
                title = "Finance Page Accent",
                valueDisplay = activeFinanceColorName,
                onClick = {
                    activeColorTargetSetting = "finance"
                    showColorPickerDialog = true
                },
                tag = "settings_finance_color_item"
            )
        }

        // Display Aesthetics
        SettingsCategory(title = "Appearance") {
            val themeLabel = when (darkModePreference) {
                "always_dark" -> "Always Dark Theme Style"
                "always_light" -> "Always Light Theme Style"
                else -> "System Preference Style"
            }
            SettingsClickableItem(
                icon = Icons.Default.Brightness4,
                title = "Interface Style Mode",
                valueDisplay = themeLabel,
                onClick = { showThemeDialog = true },
                tag = "settings_theme_item"
            )
        }

        // Security & Privacy Logs Protection
        SettingsCategory(title = "Application Log Privacy & Security") {
            SettingsToggleItem(
                icon = Icons.Default.Security,
                title = "Secure Private Logs Access",
                subtitle = "Require an authentication PIN code to unlock and browse calculation logs.",
                checked = isHistoryLocked,
                onCheckedChange = { settingsRepository.setHistoryLocked(it) },
                tag = "settings_history_lock_toggle"
            )

            if (isHistoryLocked) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsClickableItem(
                    icon = Icons.Default.Lock,
                    title = "Change Security PIN",
                    valueDisplay = "Current PIN: ****",
                    onClick = { showPinDialog = true },
                    tag = "settings_change_pin_btn"
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Sandbox Privacy",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sandbox Isolation: Relational history records are securely contained under private UID access rights. Malicious apps or external scripts cannot scan or read your database.",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Persistent data management
        SettingsCategory(title = "Local History Database") {
            SettingsClickableItem(
                icon = Icons.Default.DeleteForever,
                title = "Empty calculation logs",
                valueDisplay = "Permanently wipe logs on device",
                onClick = { showDeleteConfirmDialog = true },
                tag = "settings_clear_history_item"
            )
        }

        // About block
        SettingsCategory(title = "Description") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp).padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("dhruv Calculator & Conversions", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Version 1.1.0", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Designed with Material Design 3 for deep accessibility, precise scientific calculation parameters, offline length and mass equations, and cached exchange tickers. Remains highly visible in all conditions.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }

    // --- POPUP DIALOGS ---

    // Dark preference
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Color Interface Theme Mode", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val prefs = listOf(
                        "system" to "System Defaults",
                        "always_dark" to "Always Dark Theme Mode",
                        "always_light" to "Always Light Theme Mode"
                    )
                    prefs.forEach { (key, name) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsRepository.setDarkModePreference(key)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            RadioButton(
                                selected = darkModePreference == key,
                                onClick = {
                                    settingsRepository.setDarkModePreference(key)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Decimal rounding selection
    if (showPrecisionDialog) {
        AlertDialog(
            onDismissRequest = { showPrecisionDialog = false },
            title = { Text("Decimal Rounding Precision", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val precs = listOf(2, 3, 4, 5, 6, 8)
                    precs.forEach { num ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsRepository.setDecimalPrecision(num)
                                    showPrecisionDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp)
                        ) {
                            RadioButton(
                                selected = decimalPrecision == num,
                                onClick = {
                                    settingsRepository.setDecimalPrecision(num)
                                    showPrecisionDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("$num significant digits", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrecisionDialog = false }) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Delete history confirmation
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Clear Local Records?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure? This deletes calculations, logs, and database histories permanently.", fontSize = 15.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        calculatorViewModel.clearHistory()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Records")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Change PIN Dialog
    if (showPinDialog) {
        var tempPin by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set Log Security PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose a 4-digit numeric code to unlock your private log records:", fontSize = 13.sp)
                    OutlinedTextField(
                        value = tempPin,
                        onValueChange = { newVal ->
                            if (newVal.all { it.isDigit() } && newVal.length <= 4) {
                                tempPin = newVal
                                pinError = null
                            }
                        },
                        label = { Text("Enter 4-digit PIN") },
                        isError = pinError != null,
                        supportingText = {
                            if (pinError != null) {
                                Text(pinError!!, color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("Only numeric digits are permitted")
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        placeholder = { Text("e.g., 1234") },
                        modifier = Modifier.fillMaxWidth().testTag("settings_pin_textfield")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempPin.length == 4) {
                            settingsRepository.setHistoryPinCode(tempPin)
                            showPinDialog = false
                        } else {
                            pinError = "PIN must be exactly 4 digits long."
                        }
                    }
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Dismiss")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showColorPickerDialog && activeColorTargetSetting != null) {
        val target = activeColorTargetSetting!!
        val currentSelId = when (target) {
            "calculator" -> calculatorColor
            "converter" -> converterColor
            "date" -> dateColor
            else -> financeColor
        }
        AlertDialog(
            onDismissRequest = { showColorPickerDialog = false },
            title = { Text("Select Accent Palette", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColorOptions.forEach { opt ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when (target) {
                                        "calculator" -> settingsRepository.setCalculatorColor(opt.id)
                                        "converter" -> settingsRepository.setConverterColor(opt.id)
                                        "date" -> settingsRepository.setDateColor(opt.id)
                                        "finance" -> settingsRepository.setFinanceColor(opt.id)
                                    }
                                    showColorPickerDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp)
                        ) {
                            RadioButton(
                                selected = currentSelId == opt.id,
                                onClick = {
                                    when (target) {
                                        "calculator" -> settingsRepository.setCalculatorColor(opt.id)
                                        "converter" -> settingsRepository.setConverterColor(opt.id)
                                        "date" -> settingsRepository.setDateColor(opt.id)
                                        "finance" -> settingsRepository.setFinanceColor(opt.id)
                                    }
                                    showColorPickerDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(opt.darkPrimary)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(opt.name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorPickerDialog = false }) {
                    Text("Dismiss")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }


}

@Composable
fun SettingsCategory(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun SettingsToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
fun SettingsClickableItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    valueDisplay: String,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(valueDisplay, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}
