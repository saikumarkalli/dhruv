package com.dhruv.finance.app.ui.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhruv.core.flags.FeatureFlagResolver
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.ui.FeatureHost
import com.dhruv.core.ui.components.NotConfiguredCard
import com.dhruv.core.ui.components.NxTopBar
import com.dhruv.finance.app.ui.settings.AccountSettingsScreen
import com.dhruv.finance.app.ui.settings.AppSettingsScreen
import com.dhruv.finance.app.ui.settings.AppSettingsViewModel
import com.dhruv.finance.app.ui.settings.ModuleSettingsScreen
import com.dhruv.finance.app.ui.settings.SettingsContributionSource
import com.dhruv.finance.app.ui.settings.SettingsScreen
import com.dhruv.finance.assistant.AssistantScreen
import com.dhruv.finance.assistant.AssistantViewModel
import com.dhruv.finance.currency.CurrencyScreen
import com.dhruv.finance.currency.CurrencyViewModel
import com.dhruv.finance.date.DateScreen
import com.dhruv.finance.date.DateViewModel
import com.dhruv.finance.time.TimeScreen
import com.dhruv.finance.time.TimeViewModel
import com.dhruv.finance.unit.UnitScreen
import com.dhruv.finance.unit.UnitViewModel
import com.dhruv.settings.SettingsRepository
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Render side for the shell-level "detail/utility" [DetailRoute]s (ADR-0024): each of these used to
 * be a bare pager tab in the old 5-tab [com.dhruv.finance.app.MainActivity]; now they are swapped in
 * full-screen over the DhruvNext 4-tab scaffold, so each gets its own [NxTopBar] back bar instead of
 * relying on the pager/bottom-nav for navigation. The wrapped feature screen/[FeatureHost] pairing is
 * unchanged from the previous tab content — only the chrome around it changed.
 */
@Composable
fun SettingsDetailContent(
    subRoute: DetailRoute?,
    resolver: FeatureFlagResolver,
    crashReporter: CrashReporter,
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
    onOpenSubRoute: (DetailRoute) -> Unit,
    onBackFromSubRoute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Settings is app-shell, not a feature module: no feature flag of its own — matches its
    // existing unwrapped usage in MainActivity. FR-009: one back step from any settings screen to
    // the Settings top level (subRoute -> null), one more to the tab (top level -> onBack).
    Column(modifier = modifier.fillMaxSize()) {
        when (subRoute) {
            null -> {
                NxTopBar(title = "Settings", onBack = onBack)
                val appSettingsViewModel: AppSettingsViewModel = koinViewModel()
                SettingsScreen(
                    settingsRepository = settingsRepository,
                    appSettingsViewModel = appSettingsViewModel,
                    resolver = resolver,
                    onOpenSubRoute = onOpenSubRoute,
                )
            }
            DetailRoute.SettingsAccount -> {
                NxTopBar(title = "Account", onBack = onBackFromSubRoute)
                // 0b.2 (T046): the real AccountSettingsScreen replaces SettingsAccountBody
                // (0b.1's zero-regression stand-in) and resolves its own ViewModel via Koin — which
                // is what let the consentRepository/trackerAccountRepository/sessionStore params
                // drop off this function and the three MainActivity signatures above it.
                AccountSettingsScreen()
            }
            DetailRoute.SettingsApp -> {
                NxTopBar(title = "App", onBack = onBackFromSubRoute)
                val appSettingsViewModel: AppSettingsViewModel = koinViewModel()
                AppSettingsScreen(settingsRepository = settingsRepository, appSettingsViewModel = appSettingsViewModel)
            }
            is DetailRoute.SettingsModule -> {
                val contributionSource: SettingsContributionSource = koinInject()
                val contribution = contributionSource.resolve().firstOrNull { it.moduleKey == subRoute.moduleKey }
                NxTopBar(
                    title = contribution?.let { stringResource(it.title) } ?: subRoute.moduleKey,
                    onBack = onBackFromSubRoute,
                )
                if (contribution != null) {
                    ModuleSettingsScreen(
                        contribution = contribution,
                        crashReporter = crashReporter,
                        settingsRepository = settingsRepository,
                        resolver = resolver,
                        onRequestConsent = { onOpenSubRoute(DetailRoute.SettingsAccount) },
                    )
                } else {
                    NotConfiguredCard(
                        message = "This module has no settings entry.",
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
            // Every other DetailRoute is rendered by its own branch in DetailRouteContent — never
            // reached here, since MainActivity only calls SettingsDetailContent for the Settings route.
            else -> Unit
        }
    }
}

/**
 * D7-bridge: wires the existing [AssistantScreen] so the floating "Ask Dhruv" pill has a real
 * destination today. A DhruvNext-styled chat screen replaces this content in a later phase (D7).
 */
/**
 * FR-032's actual enforcement point: a module the user turned off (`module_enabled_<moduleKey>`)
 * is removed from content the same way a flag-disabled one is — `FeatureHost` renders
 * `FeatureDisabledCard` (PLATFORM.md §4), rather than inventing a second "turned off" treatment.
 *
 * Before this existed the on/off toggle in `ModuleSettingsScreen` wrote a preference **nothing
 * read**, so turning a module off changed only that settings screen's own appearance — a row that
 * appeared operable and did essentially nothing (SC-011). Only the three modules that declare
 * `optional = true` reach this; `calculator` is the Calc tab's content and is never hideable
 * (FR-033).
 */
@Composable
private fun rememberModuleEnabled(moduleKey: String): Boolean {
    val settingsRepository: SettingsRepository = koinInject()
    val enabled by settingsRepository.isModuleEnabled(moduleKey).collectAsStateWithLifecycle(initialValue = true)
    return enabled
}

@Composable
fun AskDetailContent(
    resolver: FeatureFlagResolver,
    crashReporter: CrashReporter,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        NxTopBar(title = "Ask Dhruv", onBack = onBack)
        val vm: AssistantViewModel = koinViewModel()
        val error by vm.featureError.collectAsStateWithLifecycle()
        // Both evaluated before the call — a @Composable must never sit behind a
        // short-circuiting `&&`, or its invocation count varies between recompositions.
        val moduleEnabled = rememberModuleEnabled("assistant")
        FeatureHost("assistant", resolver.isEnabled("assistant") && moduleEnabled, error, crashReporter) {
            AssistantScreen(viewModel = vm)
        }
    }
}

@Composable
fun CurrencyDetailContent(
    resolver: FeatureFlagResolver,
    crashReporter: CrashReporter,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // No "favourite this pair" behaviour exists yet, so the action renders disabled
        // (onTrailingClick = null dims it via NxTopBar, per DhruvNext §6.6) rather than faking
        // persistence that doesn't exist.
        NxTopBar(
            title = "Currency",
            onBack = onBack,
            trailingIcon = Icons.Default.StarBorder,
            trailingIconContentDescription = "Favourite this currency pair",
            onTrailingClick = null,
        )
        val vm: CurrencyViewModel = koinViewModel()
        val error by vm.featureError.collectAsStateWithLifecycle()
        // Both evaluated before the call — a @Composable must never sit behind a
        // short-circuiting `&&`, or its invocation count varies between recompositions.
        val moduleEnabled = rememberModuleEnabled("currency")
        FeatureHost("currency", resolver.isEnabled("currency") && moduleEnabled, error, crashReporter) {
            CurrencyScreen(viewModel = vm)
        }
    }
}

@Composable
fun UnitDetailContent(
    resolver: FeatureFlagResolver,
    crashReporter: CrashReporter,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        NxTopBar(title = "Units", onBack = onBack)
        val vm: UnitViewModel = koinViewModel()
        val error by vm.featureError.collectAsStateWithLifecycle()
        // Both evaluated before the call — a @Composable must never sit behind a
        // short-circuiting `&&`, or its invocation count varies between recompositions.
        val moduleEnabled = rememberModuleEnabled("unit")
        FeatureHost("unit", resolver.isEnabled("unit") && moduleEnabled, error, crashReporter) {
            UnitScreen(viewModel = vm)
        }
    }
}

@Composable
fun DateDetailContent(
    resolver: FeatureFlagResolver,
    crashReporter: CrashReporter,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        NxTopBar(title = "Date", onBack = onBack)
        val vm: DateViewModel = koinViewModel()
        val error by vm.featureError.collectAsStateWithLifecycle()
        FeatureHost("date", resolver.isEnabled("date"), error, crashReporter) {
            // settingsRepository takes its koinInject() default; not passed explicitly here.
            DateScreen(viewModel = vm)
        }
    }
}

@Composable
fun TimeDetailContent(
    resolver: FeatureFlagResolver,
    crashReporter: CrashReporter,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        NxTopBar(title = "Time", onBack = onBack)
        val vm: TimeViewModel = koinViewModel()
        val error by vm.featureError.collectAsStateWithLifecycle()
        FeatureHost("time", resolver.isEnabled("time"), error, crashReporter) {
            TimeScreen(viewModel = vm)
        }
    }
}
