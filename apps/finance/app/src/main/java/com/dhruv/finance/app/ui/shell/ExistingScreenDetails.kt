package com.dhruv.finance.app.ui.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhruv.core.flags.FeatureFlagResolver
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.ui.FeatureHost
import com.dhruv.core.ui.components.NxTopBar
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

/**
 * Render side for the shell-level "detail/utility" [DetailRoute]s (ADR-0024): each of these used to
 * be a bare pager tab in the old 5-tab [com.dhruv.finance.app.MainActivity]; now they are swapped in
 * full-screen over the DhruvNext 4-tab scaffold, so each gets its own [NxTopBar] back bar instead of
 * relying on the pager/bottom-nav for navigation. The wrapped feature screen/[FeatureHost] pairing is
 * unchanged from the previous tab content — only the chrome around it changed.
 */
@Composable
fun SettingsDetailContent(
    settingsRepository: SettingsRepository,
    onClearHistory: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Settings is app-shell, not a feature module: no feature flag, no ViewModel, no FeatureHost —
    // matches its existing unwrapped usage in MainActivity.
    Column(modifier = modifier.fillMaxSize()) {
        NxTopBar(title = "Settings", onBack = onBack)
        SettingsScreen(
            settingsRepository = settingsRepository,
            onClearHistory = onClearHistory,
        )
    }
}

/**
 * D7-bridge: wires the existing [AssistantScreen] so the floating "Ask Dhruv" pill has a real
 * destination today. A DhruvNext-styled chat screen replaces this content in a later phase (D7).
 */
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
        FeatureHost("assistant", resolver.isEnabled("assistant"), error, crashReporter) {
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
        NxTopBar(title = "Currency", onBack = onBack)
        val vm: CurrencyViewModel = koinViewModel()
        val error by vm.featureError.collectAsStateWithLifecycle()
        FeatureHost("currency", resolver.isEnabled("currency"), error, crashReporter) {
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
        FeatureHost("unit", resolver.isEnabled("unit"), error, crashReporter) {
            // UnitScreen takes no modifier parameter.
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
