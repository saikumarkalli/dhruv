package com.dhruv.finance.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dhruv.core.flags.FeatureFlagResolver
import com.dhruv.core.navigation.NavTarget
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.ui.FeatureHost
import com.dhruv.core.ui.components.EmptyStateCard
import com.dhruv.core.ui.components.ListGroupRow
import com.dhruv.core.ui.components.SwitchRow
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.finance.app.R
import com.dhruv.settings.SettingsRepository
import com.dhruv.settings.contribution.SettingsContribution
import kotlinx.coroutines.launch

/** No module needs a consent gate unless its own [FeatureFlagResolver] says so — a safe default
 * for call sites (tests) that don't care about consent gating. */
private val NoConsentResolver =
    object : FeatureFlagResolver {
        override fun isEnabled(key: String) = true
    }

/**
 * Renders **one** [SettingsContribution] — its own on/off control (FR-032, `SET-BR-005`) first,
 * then either the consent-needed state (FR-035, `SET-UI-008`) or its ungrouped rows and each group
 * under its own label (FR-029, contract §1 rule 4: submodules are groups inside the entry, never
 * sibling top-level destinations). Wrapped in [FeatureHost] keyed on the contribution's `moduleKey`
 * (contract §4 rule 12, `SET-ARCH-007`): a row whose Flow throws while this entry is producing its
 * rows reports through [SettingsRowRenderer]'s `onError` rather than crashing, degrading this one
 * entry to its own [com.dhruv.core.ui.FeatureErrorCard] — every other entry keeps working, since
 * each `ModuleSettingsScreen` instance catches only its own contribution's failures.
 */
@Composable
fun ModuleSettingsScreen(
    contribution: SettingsContribution,
    crashReporter: CrashReporter,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
    resolver: FeatureFlagResolver = NoConsentResolver,
    onNavigate: (NavTarget) -> Unit = {},
    onRequestConsent: () -> Unit = {},
) {
    var caughtError by remember(contribution.moduleKey) { mutableStateOf<Throwable?>(null) }

    FeatureHost(
        featureKey = contribution.moduleKey,
        isEnabled = true,
        featureError = caughtError,
        crashReporter = crashReporter,
    ) {
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(DhruvNextSpacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            val moduleEnabled by settingsRepository.isModuleEnabled(contribution.moduleKey).collectAsState(initial = true)
            // FR-032 offers this control for **optional** modules only — FR-033 forbids making a
            // primary navigation destination user-hideable, and `calculator` is the Calc tab's own
            // content despite its `moduleKey` not matching any `TabKey` name. A non-optional
            // module is always on, so its `moduleEnabled` is pinned true below rather than read.
            if (contribution.optional) {
                ModuleEnableRow(
                    moduleKey = contribution.moduleKey,
                    enabled = moduleEnabled,
                    settingsRepository = settingsRepository,
                )
            }
            val effectivelyEnabled = !contribution.optional || moduleEnabled

            // Both collected unconditionally, above the `when` — a @Composable call must never sit
            // inside a branch condition or behind a short-circuiting `&&`, or its invocation count
            // varies between recompositions and corrupts the slot table. (This read used to live
            // inline in the consent branch's condition, where `!moduleEnabled` matching first, or
            // `requiresConsent` returning false, skipped the call entirely.)
            val consentGranted by contribution.consentGranted.collectAsState(initial = true)
            val consentGateApplies = resolver.requiresConsent(contribution.moduleKey) && !consentGranted

            when {
                // FR-034/`SET-UI-011`: content hidden by this module's own on/off setting shows an
                // empty state pointing back at it — the toggle right above is the way back, never a
                // blank screen. This is also what keeps the entry reachable after being turned off:
                // it stays in the modules tier (see settings-contribution.md's implementation note),
                // it just shows this instead of its rows.
                !effectivelyEnabled ->
                    EmptyStateCard(
                        message = stringResource(R.string.settings_module_disabled_empty),
                        modifier = Modifier.fillMaxSize(),
                    )

                consentGateApplies ->
                    ConsentNeededRow(contribution = contribution, onRequestConsent = onRequestConsent)

                else ->
                    contribution.groups.forEach { group ->
                        LabeledSettingsGroup(
                            label = group.label,
                            rows =
                                group.rows.map { row ->
                                    { SettingsRowRenderer(row = row, onNavigate = onNavigate, onError = { caughtError = it }) }
                                },
                        )
                    }
            }
        }
    }
}

/** FR-032/`SET-BR-005`: every non-primary-nav module offers this. Off retains the module's other
 * stored preferences — this row writes only `module_enabled_<moduleKey>`, nothing else. */
@Composable
private fun ModuleEnableRow(
    moduleKey: String,
    enabled: Boolean,
    settingsRepository: SettingsRepository,
) {
    val scope = rememberCoroutineScope()
    SwitchRow(
        label = stringResource(R.string.settings_module_enable_label),
        description = stringResource(R.string.settings_module_enable_description),
        checked = enabled,
        onCheckedChange = { newValue -> scope.launch { settingsRepository.setModuleEnabled(moduleKey, newValue) } },
    )
}

/** FR-035/`SET-UI-008`: states which consent is needed and offers the route to grant it, rather
 * than rendering the module's controls inert. */
@Composable
private fun ConsentNeededRow(
    contribution: SettingsContribution,
    onRequestConsent: () -> Unit,
) {
    val message = contribution.consentRequiredMessage?.let { stringResource(it) } ?: stringResource(contribution.summary)
    ListGroupRow(
        title = stringResource(R.string.settings_module_consent_needed_title),
        subtitle = message,
        onClick = onRequestConsent,
    )
}
