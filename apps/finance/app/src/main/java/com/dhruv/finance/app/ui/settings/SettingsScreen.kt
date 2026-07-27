package com.dhruv.finance.app.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.components.InitialsTile
import com.dhruv.core.ui.components.ListGroup
import com.dhruv.core.ui.components.ListGroupRow
import com.dhruv.core.ui.components.NxCard
import com.dhruv.core.ui.components.SectionLabel
import com.dhruv.core.ui.components.SwitchRow
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import com.dhruv.settings.AppSettings
import com.dhruv.settings.SettingsRepository
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Settings — DhruvNext §6.9 (ADR-0024): Account / Appearance / Money / Privacy & data / App.
 * App-shell screen, not a feature module: no feature flag, no ViewModel, no `FeatureHost` — the
 * caller ([com.dhruv.finance.app.ui.shell.SettingsDetailContent], a shell-level detail route under
 * the DhruvNext 4-tab pager) renders the back-top-bar; this composable is the scrollable body only.
 */
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val appSettings by settingsRepository.observe().collectAsState(initial = AppSettings())

    val uiState =
        SettingsUiState(
            isDegree = settingsRepository.isDegree.collectAsState(initial = true).value,
            darkModePreference = settingsRepository.darkModePreference.collectAsState(initial = "system").value,
            decimalPrecision = settingsRepository.decimalPrecision.collectAsState(initial = 4).value,
            formatLocale = settingsRepository.formatLocale.collectAsState(initial = "international").value,
            isHistoryLocked = settingsRepository.isHistoryLocked.collectAsState(initial = false).value,
            historyPinCode = settingsRepository.historyPinCode.collectAsState(initial = "").value,
            accentColorHex = appSettings.accentColorHex,
            biometricEnabled = appSettings.biometricEnabled,
        )

    // Dynamic version info (unchanged).
    val context = LocalContext.current
    val packageInfo = remember { context.packageManager.getPackageInfo(context.packageName, 0) }
    val dynamicVersionName = packageInfo.versionName ?: "1.0"
    val dynamicVersionCode =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode
        }

    // Dialog & sheet states (unchanged; the old "Appearance & Colors" sheet trigger is gone —
    // Appearance is inline in the body now).
    var showPrecisionSheet by remember { mutableStateOf(false) }
    var showLocaleDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    val colors = LocalDhruvNextColors.current
    val accentSwatches = remember { defaultAccentSwatches() }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(DhruvNextSpacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // ── Account ───────────────────────────────────────────────────────────────────────────
        // No auth/sync system exists yet (Supabase auth is a future phase) — an honest placeholder,
        // not fabricated sync data.
        NxCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsTile(name = "Local device")
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "Local device", color = colors.tx, fontWeight = FontWeight.Bold, fontSize = DhruvNextType.title)
                    Text(text = "Local device only — no account yet", color = colors.tx3, fontSize = DhruvNextType.meta)
                }
            }
        }

        // ── Appearance ────────────────────────────────────────────────────────────────────────
        SettingsGroup(title = "Appearance") {
            ListGroup(
                rows =
                    listOf(
                        {
                            AppearanceThemeRow(
                                darkModePreference = uiState.darkModePreference,
                                onThemeChanged = { settingsRepository.setDarkModePreference(it) },
                            )
                        },
                        {
                            AccentColorPickerRow(
                                swatches = accentSwatches,
                                selectedHex = uiState.accentColorHex,
                                onColorSelected = { hex ->
                                    coroutineScope.launch {
                                        settingsRepository.update { copy(accentColorHex = hex) }
                                    }
                                },
                            )
                        },
                        {
                            // No dynamic-color (Material You) infrastructure exists anywhere in this
                            // codebase yet — future hookup once that plumbing lands.
                            DisabledSwitchRow(
                                label = "Use wallpaper colours",
                                description = "Match your device's Material You palette — coming soon",
                            )
                        },
                    ),
            )
        }

        // ── Money ─────────────────────────────────────────────────────────────────────────────
        SettingsGroup(title = "Money") {
            val precisionPattern = if (uiState.decimalPrecision > 0) "#." + "#".repeat(uiState.decimalPrecision) else "#"
            val precisionPreview = DecimalFormat(precisionPattern, DecimalFormatSymbols(Locale.US)).format(12.3456789)
            ListGroup(
                rows =
                    listOf(
                        {
                            ListGroupRow(
                                title = "Number format",
                                subtitle = null,
                                onClick = { showLocaleDialog = true },
                                trailing = {
                                    Text(
                                        text = if (uiState.formatLocale == "indian") "Indian" else "International",
                                        color = colors.tx2,
                                        fontSize = DhruvNextType.body,
                                    )
                                },
                                modifier = Modifier.testTag("settings_locale_item"),
                            )
                        },
                        {
                            ListGroupRow(
                                title = "Decimal precision",
                                onClick = { showPrecisionSheet = true },
                                trailing = {
                                    Text(
                                        "${uiState.decimalPrecision} places",
                                        color = colors.tx2,
                                        fontSize = DhruvNextType.body,
                                    )
                                },
                                modifier = Modifier.testTag("settings_precision_item"),
                            )
                        },
                        {
                            ListGroupRow(
                                title = "Preview",
                                showChevron = false,
                                trailing = {
                                    Text(
                                        precisionPreview,
                                        color = colors.acc,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = DhruvNextType.cardTitle,
                                    )
                                },
                            )
                        },
                        {
                            LabeledSegmentedRow(
                                label = "Angle mode",
                                options = listOf("DEG", "RAD"),
                                selectedIndex = if (uiState.isDegree) 0 else 1,
                                onSelected = { index -> settingsRepository.setDegree(index == 0) },
                            )
                        },
                    ),
            )
        }

        // ── Privacy & data ────────────────────────────────────────────────────────────────────
        // No "Ask Dhruv" persisted consent flag exists (the assistant keeps its own in-memory
        // consent, a known gap tracked separately) — omitted rather than fabricated here.
        SettingsGroup(title = "Privacy & data") {
            ListGroup(
                rows =
                    buildList<@Composable () -> Unit> {
                        add {
                            SwitchRow(
                                label = "App lock",
                                description = "Saves your preference now — biometric lock-screen enforcement is coming soon",
                                checked = uiState.biometricEnabled,
                                onCheckedChange = { enabled ->
                                    coroutineScope.launch {
                                        settingsRepository.update { copy(biometricEnabled = enabled) }
                                    }
                                },
                            )
                        }
                        add {
                            SwitchRow(
                                label = "Lock history",
                                description = "Require a PIN to view saved calculator results",
                                checked = uiState.isHistoryLocked,
                                onCheckedChange = { locked ->
                                    if (locked && uiState.historyPinCode.isEmpty()) {
                                        showPinDialog = true
                                    } else {
                                        settingsRepository.setHistoryLocked(locked)
                                    }
                                },
                            )
                        }
                        if (uiState.isHistoryLocked) {
                            add {
                                ListGroupRow(
                                    title = "Change PIN",
                                    onClick = { showPinDialog = true },
                                    trailing = { Text("••••", color = colors.tx2, fontSize = DhruvNextType.body) },
                                )
                            }
                        }
                        add {
                            PlaceholderRow(
                                title = "Export my data",
                                subtitle = "Download a copy of everything stored on this device",
                            )
                        }
                        add {
                            DangerRow(
                                title = "Delete everything",
                                subtitle = "Erase all account data",
                                trailingLabel = "Soon",
                            )
                        }
                        add {
                            DangerRow(
                                title = "Clear history",
                                subtitle = "Permanently wipe saved calculator results",
                                onClick = { showClearHistoryDialog = true },
                            )
                        }
                    },
            )
        }

        // ── App ───────────────────────────────────────────────────────────────────────────────
        // No per-tool "hide from nav" rows here: DhruvNext's tabs (Home/Calc/Plan/Insights) are
        // fixed system destinations, not user-toggleable — unlike the old app's optional
        // Converter/Date/Finance/Time tabs, there is nothing left for such a toggle to control.
        SettingsGroup(title = "App") {
            ListGroup(
                rows =
                    listOf(
                        {
                            ListGroupRow(
                                title = "About Dhruv Finance",
                                subtitle = "Version $dynamicVersionName (build $dynamicVersionCode)",
                                showChevron = false,
                            )
                        },
                    ),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    // --- Overlays (unchanged behaviour) ---

    if (showLocaleDialog) {
        LocaleFormatDialog(
            currentLocale = uiState.formatLocale,
            onLocaleSelected = {
                settingsRepository.setFormatLocale(it)
                showLocaleDialog = false
            },
            onDismiss = { showLocaleDialog = false },
        )
    }

    if (showPrecisionSheet) {
        SettingsPrecisionSheet(
            currentPrecision = uiState.decimalPrecision,
            onPrecisionSelected = {
                settingsRepository.setDecimalPrecision(it)
                showPrecisionSheet = false
            },
            onDismiss = { showPrecisionSheet = false },
        )
    }

    if (showPinDialog) {
        PinEntryDialog(
            onPinSaved = { pin ->
                settingsRepository.setHistoryPinCode(pin)
                settingsRepository.setHistoryLocked(true)
                showPinDialog = false
            },
            onDismiss = { showPinDialog = false },
        )
    }

    if (showClearHistoryDialog) {
        ClearHistoryDialog(
            onConfirm = {
                onClearHistory()
                showClearHistoryDialog = false
            },
            onDismiss = { showClearHistoryDialog = false },
        )
    }
}

/** A [SectionLabel] followed by its grouped content — the repeating shape of every section below. */
@Composable
private fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(text = title, modifier = Modifier.padding(start = 4.dp))
        content()
    }
}
