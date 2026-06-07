package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SettingsRepository
import com.example.ui.calculator.CalculatorViewModel
import com.example.ui.theme.appGradientBackground
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    calculatorViewModel: CalculatorViewModel,
    appVersion: String = "1.0",
    appVersionCode: Int = 1,
    modifier: Modifier = Modifier
) {
    // Collect State
    val uiState = SettingsUiState(
        isDegree = settingsRepository.isDegree.collectAsState(initial = true).value,
        darkModePreference = settingsRepository.darkModePreference.collectAsState(initial = "system").value,
        decimalPrecision = settingsRepository.decimalPrecision.collectAsState(initial = 4).value,
        formatLocale = settingsRepository.formatLocale.collectAsState(initial = "international").value,
        isHistoryLocked = settingsRepository.isHistoryLocked.collectAsState(initial = false).value,
        historyPinCode = settingsRepository.historyPinCode.collectAsState(initial = "").value,
        isConverterEnabled = settingsRepository.isConverterEnabled.collectAsState(initial = true).value,
        isDateEnabled = settingsRepository.isDateEnabled.collectAsState(initial = true).value,
        isFinanceEnabled = settingsRepository.isFinanceEnabled.collectAsState(initial = true).value,
        calculatorColor = settingsRepository.calculatorColor.collectAsState(initial = "cyan").value,
        converterColor = settingsRepository.converterColor.collectAsState(initial = "purple").value,
        dateColor = settingsRepository.dateColor.collectAsState(initial = "coral").value,
        financeColor = settingsRepository.financeColor.collectAsState(initial = "amber").value,
        isTimeEnabled = settingsRepository.isTimeEnabled.collectAsState(initial = true).value,
        timeColor = settingsRepository.timeColor.collectAsState(initial = "teal").value
    )

    // Dialog & Sheet States
    var showPrecisionSheet by remember { mutableStateOf(false) }
    var showAppearanceSheet by remember { mutableStateOf(false) }
    var showLocaleDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    
    var activeSectionConfig by remember { mutableStateOf<SettingsSectionConfig?>(null) }

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
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }


        // General
        SettingsCategory(title = "General") {
            SettingsClickableItem(
                title = "Number format",
                valueDisplay = if (uiState.formatLocale == "indian") "Indian" else "International",
                onClick = { showLocaleDialog = true },
                tag = "settings_locale_item"
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            SettingsClickableItem(
                title = "Decimal precision",
                valueDisplay = "${uiState.decimalPrecision} places",
                onClick = { showPrecisionSheet = true },
                tag = "settings_precision_item"
            )
        }

        // Calculator
        SettingsCategory(title = "Calculator") {
            SettingsSegmentedControlItem(
                title = "Angle mode",
                options = listOf("DEG", "RAD"),
                selectedIndex = if (uiState.isDegree) 0 else 1,
                onOptionSelected = { settingsRepository.setDegree(it == 0) }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Preview", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                val pattern = if (uiState.decimalPrecision > 0) "#." + "#".repeat(uiState.decimalPrecision) else "#"
                val df = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US))
                Text(df.format(12.3456789), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }

        // Sections
        SettingsCategory(title = "Sections") {
            SettingsClickableItem(
                title = "Calculator",
                valueDisplay = "Always on",
                showChevron = false,
                onClick = { },
                tag = "settings_section_calc"
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            SettingsClickableItem(
                title = "Converter",
                valueDisplay = if (uiState.isConverterEnabled) "On" else "Off",
                onClick = {
                    activeSectionConfig = SettingsSectionConfig(
                        "converter", "Converter", uiState.isConverterEnabled, true, SettingsConstants.CONVERTER_TOOLS
                    )
                },
                tag = "settings_section_conv"
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            SettingsClickableItem(
                title = "Date & Time",
                valueDisplay = if (uiState.isDateEnabled) "On" else "Off",
                onClick = {
                    activeSectionConfig = SettingsSectionConfig(
                        "date", "Date & Time", uiState.isDateEnabled, true, SettingsConstants.DATE_TOOLS
                    )
                },
                tag = "settings_section_date"
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            SettingsClickableItem(
                title = "Finance",
                valueDisplay = if (uiState.isFinanceEnabled) "On" else "Off",
                onClick = {
                    activeSectionConfig = SettingsSectionConfig(
                        "finance", "Finance", uiState.isFinanceEnabled, true, SettingsConstants.FINANCE_TOOLS
                    )
                },
                tag = "settings_section_fin"
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            SettingsClickableItem(
                title = "Time Tools",
                valueDisplay = if (uiState.isTimeEnabled) "On" else "Off",
                onClick = {
                    activeSectionConfig = SettingsSectionConfig(
                        "time", "Time Tools", uiState.isTimeEnabled, true, SettingsConstants.TIME_TOOLS
                    )
                },
                tag = "settings_section_time"
            )
        }

        // Appearance
        SettingsCategory(title = "Appearance") {
            SettingsClickableItem(
                title = "Appearance & Colors",
                valueDisplay = "Theme & Accents",
                onClick = { showAppearanceSheet = true },
                tag = "settings_appearance_item"
            )
        }

        // Privacy & Data
        SettingsCategory(title = "Privacy & Data") {
            SettingsToggleItem(
                title = "Lock history",
                checked = uiState.isHistoryLocked,
                onCheckedChange = { locked ->
                    if (locked && uiState.historyPinCode.isEmpty()) {
                        showPinDialog = true
                    } else {
                        settingsRepository.setHistoryLocked(locked)
                    }
                },
                tag = "settings_history_lock"
            )
            if (uiState.isHistoryLocked) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                SettingsClickableItem(
                    title = "Change PIN",
                    valueDisplay = "****",
                    onClick = { showPinDialog = true },
                    tag = "settings_change_pin"
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            SettingsClickableItem(
                title = "Clear history",
                valueDisplay = "Permanently wipe",
                onClick = { showClearHistoryDialog = true },
                tag = "settings_clear_history"
            )
        }

        // About
        Text("About Dhruv Calc", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
        Text("Version $appVersion (build $appVersionCode)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
        Spacer(modifier = Modifier.height(24.dp))
    }

    // --- Overlays ---

    if (showLocaleDialog) {
        LocaleFormatDialog(
            currentLocale = uiState.formatLocale,
            onLocaleSelected = { settingsRepository.setFormatLocale(it); showLocaleDialog = false },
            onDismiss = { showLocaleDialog = false }
        )
    }

    if (showPrecisionSheet) {
        SettingsPrecisionSheet(
            currentPrecision = uiState.decimalPrecision,
            onPrecisionSelected = { settingsRepository.setDecimalPrecision(it); showPrecisionSheet = false },
            onDismiss = { showPrecisionSheet = false }
        )
    }

    if (showAppearanceSheet) {
        SettingsAppearanceSheet(
            uiState = uiState,
            onThemeChanged = { settingsRepository.setDarkModePreference(it) },
            onColorChanged = { section, colorId ->
                when (section) {
                    "calculator" -> settingsRepository.setCalculatorColor(colorId)
                    "converter" -> settingsRepository.setConverterColor(colorId)
                    "date" -> settingsRepository.setDateColor(colorId)
                    "finance" -> settingsRepository.setFinanceColor(colorId)
                    "time" -> settingsRepository.setTimeColor(colorId)
                }
            },
            onDismiss = { showAppearanceSheet = false }
        )
    }

    activeSectionConfig?.let { config ->
        SettingsSectionDetailSheet(
            config = config,
            settingsRepository = settingsRepository,
            onPageToggle = { enabled ->
                when (config.id) {
                    "converter" -> settingsRepository.setConverterEnabled(enabled)
                    "date" -> settingsRepository.setDateEnabled(enabled)
                    "finance" -> settingsRepository.setFinanceEnabled(enabled)
                    "time" -> settingsRepository.setTimeEnabled(enabled)
                }
                activeSectionConfig = activeSectionConfig?.copy(enabled = enabled)
            },
            onDismiss = { activeSectionConfig = null }
        )
    }

    if (showPinDialog) {
        PinEntryDialog(
            onPinSaved = { pin ->
                settingsRepository.setHistoryPinCode(pin)
                settingsRepository.setHistoryLocked(true)
                showPinDialog = false
            },
            onDismiss = { showPinDialog = false }
        )
    }

    if (showClearHistoryDialog) {
        ClearHistoryDialog(
            onConfirm = {
                calculatorViewModel.clearHistory()
                showClearHistoryDialog = false
            },
            onDismiss = { showClearHistoryDialog = false }
        )
    }
}
