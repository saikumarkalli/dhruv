package com.dhruv.finance.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
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
import com.dhruv.core.flags.FeatureFlagResolver
import com.dhruv.core.ui.components.EmptyStateCard
import com.dhruv.core.ui.components.ListGroup
import com.dhruv.core.ui.components.ListGroupRow
import com.dhruv.core.ui.components.SectionLabel
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.finance.app.R
import com.dhruv.finance.app.ui.shell.DetailRoute
import com.dhruv.settings.AppSettings
import com.dhruv.settings.SettingsRepository
import com.dhruv.settings.contribution.SettingsContribution
import com.dhruv.settings.contribution.SettingsRegistry
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Settings — the application control plane's top level (FR-001): the three fixed quick rows, then
 * the Account entry, then the App entry, then the modules tier assembled from every registered
 * [SettingsContribution] (FR-004) — no other inline controls (`SET-UI-004`). App-shell screen, not
 * a feature module: no feature flag, no `FeatureHost` of its own — matches Settings' existing
 * unwrapped usage in [com.dhruv.finance.app.ui.shell.SettingsDetailContent].
 */
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    appSettingsViewModel: AppSettingsViewModel,
    resolver: FeatureFlagResolver,
    onOpenSubRoute: (DetailRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val appSettings by settingsRepository.observe().collectAsState(initial = AppSettings())
    var appLockErrorMessage by remember { mutableStateOf<String?>(null) }
    val noCredentialText = stringResource(R.string.settings_app_lock_no_credential)
    val darkModePreference by settingsRepository.darkModePreference.collectAsState(initial = "system")
    val accentSwatches = remember { defaultAccentSwatches() }

    val contributionSource: SettingsContributionSource = koinInject()
    val registry: SettingsRegistry = koinInject()
    val context = LocalContext.current

    // Contract §4 rule 13: resolved once per Settings open, not per recomposition — the resolver
    // callback takes an arbitrary Int id from any registered module's contribution, so it can't be
    // a compile-time stringResource() call; a config change (e.g. locale) while this screen stays
    // open won't re-localize until it's reopened, an accepted trade-off of the "resolve once" rule.
    @Suppress("LocalContextGetResourceValueCall")
    val contributions =
        remember {
            registry.resolve(contributionSource.resolve(), resolver) { id -> context.getString(id) }
        }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(DhruvNextSpacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // ── Quick rows (FR-002) — exactly these three, operable in place ────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(text = stringResource(R.string.settings_quick_rows_section), modifier = Modifier.padding(start = 4.dp))
            ListGroup(
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
                                    coroutineScope.launch { settingsRepository.update { copy(accentColorHex = hex) } }
                                },
                            )
                        },
                        {
                            AppLockQuickRow(
                                enabled = appSettings.biometricEnabled,
                                errorMessage = appLockErrorMessage,
                                onCheckedChange = { enabled ->
                                    coroutineScope.launch {
                                        appLockErrorMessage = null
                                        appSettingsViewModel
                                            .setAppLockEnabled(enabled)
                                            .onFailure { appLockErrorMessage = noCredentialText }
                                    }
                                },
                            )
                        },
                    ),
            )
        }

        // ── Account / App entries ────────────────────────────────────────────────────────────────
        ListGroup(
            rows =
                listOf(
                    {
                        ListGroupRow(
                            title = stringResource(R.string.settings_account_entry_title),
                            subtitle = stringResource(R.string.settings_account_entry_subtitle),
                            onClick = { onOpenSubRoute(DetailRoute.SettingsAccount) },
                        )
                    },
                    {
                        ListGroupRow(
                            title = stringResource(R.string.settings_app_entry_title),
                            subtitle = stringResource(R.string.settings_app_entry_subtitle),
                            onClick = { onOpenSubRoute(DetailRoute.SettingsApp) },
                        )
                    },
                ),
        )

        // ── Modules tier (FR-004) — assembled from whatever is registered and enabled ───────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(text = stringResource(R.string.settings_modules_section), modifier = Modifier.padding(start = 4.dp))
            if (contributions.isEmpty()) {
                EmptyStateCard(
                    message = stringResource(R.string.settings_modules_empty),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                ListGroup(
                    rows =
                        contributions.map { contribution ->
                            {
                                ListGroupRow(
                                    title = stringResource(contribution.title),
                                    subtitle = stringResource(contribution.summary),
                                    onClick = { onOpenSubRoute(DetailRoute.SettingsModule(contribution.moduleKey)) },
                                )
                            }
                        },
                )
            }
        }
    }
}
