package com.dhruv.finance.app.ui.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dhruv.core.security.LockTimeout
import com.dhruv.core.ui.components.ListGroup
import com.dhruv.core.ui.components.ListGroupRow
import com.dhruv.core.ui.components.SectionLabel
import com.dhruv.core.ui.components.SegmentedRow
import com.dhruv.core.ui.components.SwitchRow
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import com.dhruv.finance.app.R
import com.dhruv.settings.AppSettings
import com.dhruv.settings.SettingsRepository
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * Settings › App tier (FR-001). Appearance (0b.1, T029), Security and Notifications (0b.3, T070/
 * T074) and App details (0b.4, T101) are all real areas now — no temporary Legacy section remains.
 * Not wrapped in `FeatureHost`: the App tier is shell-owned with no feature flag of its own, same
 * as [SettingsScreen] itself.
 */
@Composable
fun AppSettingsScreen(
    settingsRepository: SettingsRepository,
    appSettingsViewModel: AppSettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val appSettings by settingsRepository.observe().collectAsState(initial = AppSettings())
    val darkModePreference by settingsRepository.darkModePreference.collectAsState(initial = "system")
    val accentSwatches = remember { defaultAccentSwatches() }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(DhruvNextSpacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        LabeledSettingsGroup(
            label = R.string.settings_appearance_section,
            rows =
                listOf(
                    {
                        AppearanceThemeRow(
                            darkModePreference = darkModePreference,
                            onThemeChanged = { settingsRepository.setDarkModePreference(it) },
                        )
                    },
                    {
                        AccentColorPickerRow(
                            swatches = accentSwatches,
                            selectedHex = appSettings.accentColorHex,
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
                            label = stringResource(R.string.settings_wallpaper_colours_label),
                            description = stringResource(R.string.settings_wallpaper_colours_description),
                        )
                    },
                ),
        )

        SecuritySection(
            settingsRepository = settingsRepository,
            appSettingsViewModel = appSettingsViewModel,
            appSettings = appSettings,
        )

        NotificationsSection(appSettings = appSettings, appSettingsViewModel = appSettingsViewModel)

        AppDetailsSection()
    }
}

/**
 * Security area (0b.3, T070–T072, `SET-UI-001`…`SET-UI-003`, `SET-BR-013`…`SET-BR-020`): the real
 * app lock switch (credential-checked via [AppSettingsViewModel.setAppLockEnabled]), the auto-lock
 * timeout, hide-amounts, and the legacy history-lock/PIN rows kept working but labelled superseded
 * (`SET-BR-020`, T071).
 */
@Composable
private fun SecuritySection(
    settingsRepository: SettingsRepository,
    appSettingsViewModel: AppSettingsViewModel,
    appSettings: AppSettings,
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalDhruvNextColors.current
    var noCredentialMessage by remember { mutableStateOf<String?>(null) }

    var isHistoryLocked by remember { mutableStateOf(false) }
    var historyPinCode by remember { mutableStateOf("") }
    var showPinDialog by remember { mutableStateOf(false) }
    val historyLockedState by settingsRepository.isHistoryLocked.collectAsState(initial = false)
    val pinState by settingsRepository.historyPinCode.collectAsState(initial = "")
    isHistoryLocked = historyLockedState
    historyPinCode = pinState

    val noCredentialText = stringResource(R.string.settings_app_lock_no_credential)
    val timeoutOptions =
        listOf(
            LockTimeout.IMMEDIATE to stringResource(R.string.settings_timeout_immediate),
            LockTimeout.AFTER_1_MIN to stringResource(R.string.settings_timeout_after_1_min),
            LockTimeout.AFTER_5_MIN to stringResource(R.string.settings_timeout_after_5_min),
            LockTimeout.AFTER_15_MIN to stringResource(R.string.settings_timeout_after_15_min),
        )
    val selectedTimeout = LockTimeout.fromId(appSettings.appLockTimeout)

    LabeledSettingsGroup(
        label = R.string.settings_security_section,
        rows =
            buildList<@Composable () -> Unit> {
                add {
                    SwitchRow(
                        label = stringResource(R.string.settings_app_lock_label),
                        description = noCredentialMessage ?: stringResource(R.string.settings_app_lock_description),
                        checked = appSettings.biometricEnabled,
                        onCheckedChange = { enabled ->
                            coroutineScope.launch {
                                noCredentialMessage = null
                                appSettingsViewModel
                                    .setAppLockEnabled(enabled)
                                    .onFailure { noCredentialMessage = noCredentialText }
                            }
                        },
                    )
                }
                if (appSettings.biometricEnabled) {
                    add {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                            Text(
                                text = stringResource(R.string.settings_auto_lock_timeout_label),
                                color = colors.tx,
                                fontSize = DhruvNextType.cardTitle,
                            )
                            Text(
                                text = stringResource(R.string.settings_auto_lock_timeout_description),
                                color = colors.tx2,
                                fontSize = DhruvNextType.meta,
                            )
                            SegmentedRow(
                                options = timeoutOptions.map { it.second },
                                selectedIndex = timeoutOptions.indexOfFirst { it.first == selectedTimeout }.coerceAtLeast(0),
                                onSelected = { index -> appSettingsViewModel.setAutoLockTimeout(timeoutOptions[index].first.id) },
                                modifier = Modifier.fillMaxSize().padding(top = 12.dp),
                            )
                        }
                    }
                }
                add {
                    SwitchRow(
                        label = stringResource(R.string.settings_hide_amounts_label),
                        description = stringResource(R.string.settings_hide_amounts_description),
                        checked = appSettings.hideAmounts,
                        onCheckedChange = { appSettingsViewModel.setHideAmounts(it) },
                    )
                }
                add {
                    SwitchRow(
                        label = stringResource(R.string.settings_legacy_history_lock_label),
                        description = stringResource(R.string.settings_legacy_history_lock_superseded),
                        checked = isHistoryLocked,
                        onCheckedChange = { locked ->
                            if (locked && historyPinCode.isEmpty()) {
                                showPinDialog = true
                            } else {
                                settingsRepository.setHistoryLocked(locked)
                            }
                        },
                    )
                }
                if (isHistoryLocked) {
                    add {
                        ListGroupRow(
                            title = stringResource(R.string.settings_legacy_change_pin_label),
                            onClick = { showPinDialog = true },
                            trailing = {
                                Text(
                                    text = stringResource(R.string.settings_legacy_pin_masked),
                                    color = colors.tx2,
                                    fontSize = DhruvNextType.body,
                                )
                            },
                        )
                    }
                }
            },
    )

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
}

/**
 * Notifications area (0b.3, T074, `SET-UI-012`, `SET-BR-010`): system-permission state, the
 * denied banner routing to system settings, and the app-wide master switch. No per-channel row
 * here — those live in each module's own contribution (contract §3 rule 10).
 */
@Composable
private fun NotificationsSection(
    appSettings: AppSettings,
    appSettingsViewModel: AppSettingsViewModel,
) {
    val context = LocalContext.current
    // Re-read on every ON_RESUME, not once per composition (spec Edge Cases: "Notification
    // permission is granted, then revoked from the system while the app is running"). The banner's
    // own CTA sends the user to system settings to change exactly this value, so a plain
    // `remember {}` guaranteed a stale banner on the one journey it exists to support.
    var permissionDenied by remember { mutableStateOf(!NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    permissionDenied = !NotificationManagerCompat.from(context).areNotificationsEnabled()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(text = stringResource(R.string.settings_notifications_section), modifier = Modifier.padding(start = 4.dp))
        if (permissionDenied) {
            ListGroupRow(
                title = stringResource(R.string.settings_notifications_permission_denied_banner),
                subtitle = stringResource(R.string.settings_notifications_permission_denied_action),
                onClick = {
                    val intent =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
                        } else {
                            Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(android.net.Uri.fromParts("package", context.packageName, null))
                        }
                    context.startActivity(intent)
                },
            )
        }
        LabeledSettingsGroup(
            label = null,
            rows =
                listOf(
                    {
                        SwitchRow(
                            label = stringResource(R.string.settings_notifications_master_label),
                            description = stringResource(R.string.settings_notifications_master_description),
                            checked = appSettings.notificationsMaster,
                            onCheckedChange = { appSettingsViewModel.setNotificationsMaster(it) },
                        )
                    },
                ),
        )
    }
}

/**
 * App details (0b.4, T101-T103, US5): version, update check, privacy policy, third-party
 * licences, source — the old "About Dhruv Finance" row's replacement (deleted per T103).
 *
 * **No update channel exists yet** (distribution is a signed APK via GitHub Releases, ADR-0008 —
 * there is nothing to check against), so that row is absent rather than inert (FR-043) —
 * `updateChecker = null` in `appModule`'s definition. **No on-demand onboarding-replay route exists yet**
 * in the shell's `DetailRoute`/`NavTarget` system, so "Replay intro" is equally absent rather than
 * wired to nothing — both deferred, `SET-UI-014` closes **deferred** with this reason (T107).
 * Privacy policy, licences and source link to this public repo's real files (`PRIVACY.md`,
 * `third_party/`) — the repo is public since ADR-0034, so these are real, reachable destinations,
 * not fabricated ones.
 */
@Composable
private fun AppDetailsSection(appDetailsViewModel: AppDetailsViewModel = koinViewModel()) {
    val context = LocalContext.current

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
    }

    LabeledSettingsGroup(
        label = R.string.settings_app_details_section,
        rows =
            buildList<@Composable () -> Unit> {
                add {
                    ListGroupRow(
                        title = stringResource(R.string.settings_app_details_version_label),
                        subtitle =
                            stringResource(
                                R.string.settings_app_details_version_value,
                                appDetailsViewModel.versionName,
                                appDetailsViewModel.versionCode,
                            ),
                        showChevron = false,
                    )
                }
                // FR-043: absent, not inert — no UpdateChecker is wired (see this section's doc).
                if (appDetailsViewModel.updateCheckAvailable) {
                    add {
                        ListGroupRow(
                            title = stringResource(R.string.settings_app_details_update_check_label),
                            onClick = { appDetailsViewModel.checkForUpdate() },
                        )
                    }
                }
                add {
                    ListGroupRow(
                        title = stringResource(R.string.settings_app_details_privacy_policy),
                        onClick = { openUrl("https://github.com/saikumarkalli/dhruv/blob/main/PRIVACY.md") },
                    )
                }
                add {
                    ListGroupRow(
                        title = stringResource(R.string.settings_app_details_licences),
                        onClick = { openUrl("https://github.com/saikumarkalli/dhruv/tree/main/third_party") },
                    )
                }
                add {
                    ListGroupRow(
                        title = stringResource(R.string.settings_app_details_source),
                        onClick = { openUrl("https://github.com/saikumarkalli/dhruv") },
                    )
                }
            },
    )
}
