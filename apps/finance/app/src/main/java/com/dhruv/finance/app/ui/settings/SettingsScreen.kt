package com.dhruv.finance.app.ui.settings

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dhruv.core.ui.components.InitialsTile
import com.dhruv.core.ui.components.ListGroup
import com.dhruv.core.ui.components.ListGroupRow
import com.dhruv.core.ui.components.NxCard
import com.dhruv.core.ui.components.SectionLabel
import com.dhruv.core.ui.components.SwitchRow
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import com.dhruv.finance.data.tracker.auth.ConsentRepository
import com.dhruv.finance.data.tracker.auth.ConsentState
import com.dhruv.finance.data.tracker.auth.SessionState
import com.dhruv.finance.data.tracker.auth.SessionStore
import com.dhruv.finance.data.tracker.auth.TrackerAccountRepository
import com.dhruv.finance.onboarding.ConsentSwitch
import com.dhruv.finance.onboarding.OnboardingConfig
import com.dhruv.settings.AppSettings
import com.dhruv.settings.SettingsRepository
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Matches [com.dhruv.core.ui.components.InitialsTile]'s own default size, so the real avatar
 * image and its initials-tile fallback are visually interchangeable in the Account card. */
private val ACCOUNT_AVATAR_SIZE = 34.dp

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
    consentRepository: ConsentRepository,
    trackerAccountRepository: TrackerAccountRepository,
    sessionStore: SessionStore,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val appSettings by settingsRepository.observe().collectAsState(initial = AppSettings())
    val consentState by consentRepository.state.collectAsState(initial = ConsentState())
    val sessionState by sessionStore.state.collectAsState(initial = SessionState.SignedOut)

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
    var showDeleteMyDataDialog by remember { mutableStateOf(false) }
    var showDeleteMyAccountDialog by remember { mutableStateOf(false) }

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
        // Real signed-in Google identity when Active (name/email/avatar captured at sign-in,
        // SessionState.Active — found missing live: this card was still hardcoded to "Local
        // device" after a real, successful sign-in). SignedOut/Expired keep the honest
        // no-fabricated-data placeholder this card always showed before auth existed.
        NxCard(modifier = Modifier.fillMaxWidth()) {
            val active = sessionState as? SessionState.Active
            Row(verticalAlignment = Alignment.CenterVertically) {
                val avatarUrl = active?.avatarUrl
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .size(ACCOUNT_AVATAR_SIZE)
                                .clip(RoundedCornerShape(DhruvNextRadii.innerTile)),
                    )
                } else {
                    InitialsTile(name = active?.displayName ?: active?.email ?: "Local device", size = ACCOUNT_AVATAR_SIZE)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = active?.displayName ?: active?.email ?: "Local device",
                        color = colors.tx,
                        fontWeight = FontWeight.Bold,
                        fontSize = DhruvNextType.title,
                    )
                    Text(
                        text =
                            when {
                                active == null -> "Local device only — no account yet"
                                active.displayName != null && active.email != null -> active.email.orEmpty()
                                else -> "Signed in"
                            },
                        color = colors.tx3,
                        fontSize = DhruvNextType.meta,
                    )
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
                        // The three A3 consent switches (ONB-BR-004/005/006), independently
                        // revocable here outside onboarding — same copy, same ConsentRepository,
                        // same switch-to-setter mapping as OnboardingConfig/ConsentScreen's A3
                        // step (Task 3), so toggling one off here is the exact same action as
                        // never granting it during onboarding. Toggling `syncFinancialRecords` off
                        // immediately degrades SupabaseClientFactory's consent-gated data client
                        // (ONB-BR-006), so tracker screens fall back to their signed-out/no-consent
                        // state on their own without this screen doing anything else.
                        OnboardingConfig.consentSwitchCopy.forEach { copy ->
                            add {
                                SwitchRow(
                                    label = copy.label,
                                    description = copy.scopeStatement,
                                    checked = consentState.isChecked(copy.switch),
                                    onCheckedChange = { enabled ->
                                        coroutineScope.launch {
                                            when (copy.switch) {
                                                ConsentSwitch.SYNC_FINANCIAL_RECORDS ->
                                                    consentRepository.setSyncFinancialRecords(enabled)
                                                ConsentSwitch.READ_TRANSACTION_SMS ->
                                                    consentRepository.setReadTransactionSms(enabled)
                                                ConsentSwitch.ASK_DHRUV_ABOUT_MONEY ->
                                                    consentRepository.setAskDhruvAboutMoney(enabled)
                                            }
                                        }
                                    },
                                )
                            }
                        }
                        add {
                            PlaceholderRow(
                                title = "Export my data",
                                subtitle = "Download a copy of everything stored on this device",
                            )
                        }
                        // ONB-BR-008: erases tracker rows only — the account/session stays valid.
                        add {
                            DangerRow(
                                title = "Delete my data",
                                subtitle = "Erase your net worth holdings and valuations from the server",
                                onClick = { showDeleteMyDataDialog = true },
                            )
                        }
                        // ONB-BR-009: erases tracker rows + account, forces sign-out, and sends the
                        // user back through onboarding next launch (ONB-FLOW-005 covers toggling a
                        // consent switch and then reaching this in the same session).
                        add {
                            DangerRow(
                                title = "Delete my account",
                                subtitle = "Erase everything and sign out — you'll set up again next time",
                                onClick = { showDeleteMyAccountDialog = true },
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

    if (showDeleteMyDataDialog) {
        DeleteMyDataDialog(
            onConfirm = {
                showDeleteMyDataDialog = false
                coroutineScope.launch {
                    trackerAccountRepository
                        .deleteMyData()
                        .onSuccess { Toast.makeText(context, "Your data has been deleted", Toast.LENGTH_SHORT).show() }
                        .onFailure {
                            Toast.makeText(context, "Couldn't delete your data — try again", Toast.LENGTH_SHORT).show()
                        }
                }
            },
            onDismiss = { showDeleteMyDataDialog = false },
        )
    }

    if (showDeleteMyAccountDialog) {
        DeleteMyAccountDialog(
            onConfirm = {
                showDeleteMyAccountDialog = false
                coroutineScope.launch {
                    trackerAccountRepository
                        .deleteMyAccount()
                        .onSuccess { Toast.makeText(context, "Your account has been deleted", Toast.LENGTH_SHORT).show() }
                        .onFailure {
                            Toast.makeText(context, "Couldn't delete your account — try again", Toast.LENGTH_SHORT).show()
                        }
                }
            },
            onDismiss = { showDeleteMyAccountDialog = false },
        )
    }
}

/** Maps a [ConsentSwitch] case to its current on/off value — mirrors
 * [com.dhruv.finance.onboarding.ConsentScreen]'s private equivalent for A3, kept as a separate
 * copy here since that one isn't exposed outside the onboarding module. */
private fun ConsentState.isChecked(switch: ConsentSwitch): Boolean =
    when (switch) {
        ConsentSwitch.SYNC_FINANCIAL_RECORDS -> syncFinancialRecords
        ConsentSwitch.READ_TRANSACTION_SMS -> readTransactionSms
        ConsentSwitch.ASK_DHRUV_ABOUT_MONEY -> askDhruvAboutMoney
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
