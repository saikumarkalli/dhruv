package com.dhruv.finance.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DonutSmall
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dhruv.core.flags.FeatureFlagResolver
import com.dhruv.core.navigation.BackAction
import com.dhruv.core.navigation.NavTarget
import com.dhruv.core.navigation.PlanTool
import com.dhruv.core.navigation.TabKey
import com.dhruv.core.navigation.pageIndexFor
import com.dhruv.core.navigation.resolveBackAction
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.ui.FeatureHost
import com.dhruv.core.ui.components.AskPill
import com.dhruv.core.ui.components.BottomBar
import com.dhruv.core.ui.components.BottomBarTab
import com.dhruv.core.ui.components.DhruvWordmarkImage
import com.dhruv.core.ui.components.NotConfiguredCard
import com.dhruv.core.ui.theme.DhruvTheme
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import com.dhruv.finance.app.navigation.NavigationDispatcher
import com.dhruv.finance.app.ui.dashboard.DashboardScreen
import com.dhruv.finance.app.ui.onboarding.OnboardingHost
import com.dhruv.finance.app.ui.plan.PlanLauncher
import com.dhruv.finance.app.ui.settings.SettingsViewModel
import com.dhruv.finance.app.ui.shell.AppSwitcherSheet
import com.dhruv.finance.app.ui.shell.AskDetailContent
import com.dhruv.finance.app.ui.shell.CurrencyDetailContent
import com.dhruv.finance.app.ui.shell.DateDetailContent
import com.dhruv.finance.app.ui.shell.DetailRoute
import com.dhruv.finance.app.ui.shell.NotifScreen
import com.dhruv.finance.app.ui.shell.ProfileScreen
import com.dhruv.finance.app.ui.shell.SettingsDetailContent
import com.dhruv.finance.app.ui.shell.TimeDetailContent
import com.dhruv.finance.app.ui.shell.UnitDetailContent
import com.dhruv.finance.app.ui.splash.SplashScreen
import com.dhruv.finance.calculator.CalculatorScreen
import com.dhruv.finance.calculator.CalculatorViewModel
import com.dhruv.finance.calculator.copyResultToClipboard
import com.dhruv.finance.data.tracker.auth.ConsentRepository
import com.dhruv.finance.data.tracker.auth.SessionStore
import com.dhruv.finance.data.tracker.auth.TrackerAccountRepository
import com.dhruv.finance.everyday.EverydayScreen
import com.dhruv.finance.everyday.EverydayViewModel
import com.dhruv.finance.investments.InvestmentsScreen
import com.dhruv.finance.investments.InvestmentsViewModel
import com.dhruv.finance.loans.LoansScreen
import com.dhruv.finance.loans.LoansViewModel
import com.dhruv.finance.onboarding.OnboardingViewModel
import com.dhruv.finance.tax.TaxScreen
import com.dhruv.finance.tax.TaxViewModel
import com.dhruv.settings.SettingsRepository
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * The DhruvNext 5-tab shell (ADR-0024, tab set revised by ADR-0027): Home · Money · Calc · Plan ·
 * Insights, a nested `NavController` for Plan's loan/invest/tax/everyday drill-in (NAV1), and a
 * `detailRoute` overlay for the shell-level "no tab" routes (Settings/Ask/Currency/Units/Date/
 * Time/Profile/Notifications — DhruvNext §5's OWNER=null set) that render full-screen with a back
 * top bar instead of the tab bar. `SectionTheme` (ADR-0014 §8, per-tab accent) is retired here in
 * favour of one global `DhruvTheme` accent (ADR-0024 decision 2) — every tab now renders under the
 * same theme instance. Money renders `NotConfiguredCard` until its ledger screens land (Phase 3,
 * `docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md`) — same placeholder
 * pattern already used for Insights, not a second "coming soon" treatment.
 *
 * [onCreate]'s `setContent` gates this shell behind onboarding (A2 sign-in -> A3 consent -> A4
 * empty start, functional spec §5 Group A, Phase 1 onboarding build Task 3): a returning user who
 * has completed or explicitly skipped onboarding (`ConsentRepository.state.hasCompletedOnboarding`)
 * goes straight to [AppShell] exactly as before this change; everyone else sees
 * `com.dhruv.finance.app.ui.onboarding.OnboardingHost` instead — full-frame, no tab bar, no top
 * bar, same as the branded splash overlay already renders bare on top of either one.
 */
/**
 * Cold-launch / post-account-deletion onboarding gate (Task 3 decision 2; Fix 4, final
 * whole-branch review). Extracted as a pure function — no Android/Compose dependency — so it's
 * unit-testable without an Activity host: a returning user who has completed or explicitly
 * skipped onboarding this session ([exitToShell] — [OnboardingViewModel.onUseOfflineSelected] /
 * [OnboardingViewModel.onSkipEmptyStart]) sees the shell; everyone else, including a user
 * freshly routed back here by [OnboardingViewModel.resetForNewOnboardingSession] after deleting
 * their account, sees [OnboardingHost].
 */
internal fun shouldShowOnboarding(
    hasCompletedOnboarding: Boolean,
    exitToShell: Boolean,
): Boolean = !hasCompletedOnboarding && !exitToShell

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settingsRepository: SettingsRepository = koinInject()
            val resolver: FeatureFlagResolver = koinInject()
            val crashReporter: CrashReporter = koinInject()
            val navigationDispatcher: NavigationDispatcher = koinInject()
            val consentRepository: ConsentRepository = koinInject()
            val trackerAccountRepository: TrackerAccountRepository = koinInject()
            val sessionStore: SessionStore = koinInject()
            val settingsViewModel: SettingsViewModel = koinViewModel()
            val calculatorViewModel: CalculatorViewModel = koinViewModel()
            val onboardingViewModel: OnboardingViewModel = koinViewModel()

            val appSettings by settingsViewModel.settings.collectAsStateWithLifecycle()

            DhruvTheme(
                theme = appSettings.theme,
                accentColorHex = appSettings.accentColorHex,
                font = appSettings.fontFamily,
            ) {
                // The whole app UI is composed immediately and the branded splash is overlaid on
                // top of it, so the app is fully ready the instant the splash hands off.
                var showSplash by remember { mutableStateOf(true) }

                // Cold-launch gate (onboarding Task 3, decision 2): show onboarding (Splash -> A2
                // -> A3 -> A4) instead of the 5-tab shell until the user has completed or
                // explicitly skipped it. `exitToShell` is read alongside the persisted
                // `hasCompletedOnboarding` flag — the flag alone would lag by one frame behind a
                // user finishing onboarding within this same process session, since the DataStore
                // write and this recomposition aren't on the same StateFlow.
                val consentState by consentRepository.state.collectAsStateWithLifecycle()
                val onboardingExitToShell by onboardingViewModel.exitToShell.collectAsStateWithLifecycle()

                // Fix 4 (final whole-branch review, Important): `onboardingViewModel` is a single
                // Activity-scoped instance (koinViewModel() resolved once above, reused for the
                // whole process), so `exitToShell` does not reset itself when a later Settings >
                // Privacy "Delete my account" flips `hasCompletedOnboarding` back to false
                // (TrackerAccountRepositoryImpl.deleteMyAccount). Without this, the gate below
                // would stay latched on the stale `exitToShell == true` from the earlier
                // onboarding pass and keep showing the now-deleted-account's shell. Re-running on
                // every `hasCompletedOnboarding` flip (including the harmless cold-launch case,
                // where exitToShell is already false) keeps the reset unconditional on this one
                // signal rather than requiring a second, error-prone "was this a deletion" flag.
                LaunchedEffect(consentState.hasCompletedOnboarding) {
                    if (!consentState.hasCompletedOnboarding) {
                        onboardingViewModel.resetForNewOnboardingSession()
                    }
                }

                val showOnboarding = shouldShowOnboarding(consentState.hasCompletedOnboarding, onboardingExitToShell)

                Box(modifier = Modifier.fillMaxSize()) {
                    if (showOnboarding) {
                        // Pre-tab, bare, full-frame (registry §1) — never the pager/bottom-nav.
                        OnboardingHost(
                            onboardingViewModel = onboardingViewModel,
                            crashReporter = crashReporter,
                            navigationDispatcher = navigationDispatcher,
                        )
                    } else {
                        AppShell(
                            activity = this@MainActivity,
                            settingsRepository = settingsRepository,
                            consentRepository = consentRepository,
                            trackerAccountRepository = trackerAccountRepository,
                            sessionStore = sessionStore,
                            resolver = resolver,
                            crashReporter = crashReporter,
                            navigationDispatcher = navigationDispatcher,
                            calculatorViewModel = calculatorViewModel,
                            onClearHistory = { calculatorViewModel.clearHistory() },
                        )
                    }

                    if (showSplash) {
                        SplashScreen(onFinished = { showSplash = false })
                    }
                }
            }
        }
    }
}

@Composable
private fun AppShell(
    activity: ComponentActivity,
    settingsRepository: SettingsRepository,
    consentRepository: ConsentRepository,
    trackerAccountRepository: TrackerAccountRepository,
    sessionStore: SessionStore,
    resolver: FeatureFlagResolver,
    crashReporter: CrashReporter,
    navigationDispatcher: NavigationDispatcher,
    calculatorViewModel: CalculatorViewModel,
    onClearHistory: () -> Unit,
) {
    val tabs = TabKey.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    val planNavController = rememberNavController()

    var detailRoute by remember { mutableStateOf<DetailRoute?>(null) }
    var showAppSwitcher by remember { mutableStateOf(false) }

    // The only cross-feature/cross-tab navigation mechanism (NAV1/ADR-0024). Nothing in this pass
    // emits a target yet (D5's Home quick actions are the first real producer), but the shell must
    // already be listening so a later phase doesn't need to touch MainActivity again to work.
    LaunchedEffect(navigationDispatcher) {
        navigationDispatcher.targets.collect { target ->
            coroutineScope.launch { pagerState.scrollToPage(tabs.pageIndexFor(target)) }
            if (target is NavTarget.OpenPlanTool) {
                planNavController.navigate(target.tool.route())
            }
        }
    }

    // Back contract (NAV3): a shown detail route pops first, then Plan's own nested back stack
    // (only Plan has real sub-routes today), then the pager returns to page 0, then the app exits.
    DisposableEffect(activity) {
        val callback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val onPlanTab = tabs[pagerState.currentPage] == TabKey.PLAN
                    when (
                        resolveBackAction(
                            hasDetailRoute = detailRoute != null,
                            activeTabHasNestedBackStack = onPlanTab && planNavController.previousBackStackEntry != null,
                            currentTabIndex = pagerState.currentPage,
                        )
                    ) {
                        BackAction.CLOSE_DETAIL -> detailRoute = null
                        BackAction.POP_NESTED -> planNavController.popBackStack()
                        BackAction.RETURN_TO_FIRST_TAB -> coroutineScope.launch { pagerState.scrollToPage(0) }
                        BackAction.EXIT_APP -> activity.finish()
                    }
                }
            }
        activity.onBackPressedDispatcher.addCallback(activity, callback)
        onDispose { callback.remove() }
    }

    TabsScaffold(
        pagerState = pagerState,
        resolver = resolver,
        crashReporter = crashReporter,
        calculatorViewModel = calculatorViewModel,
        planNavController = planNavController,
        detailRoute = detailRoute,
        settingsRepository = settingsRepository,
        consentRepository = consentRepository,
        trackerAccountRepository = trackerAccountRepository,
        sessionStore = sessionStore,
        onClearHistory = onClearHistory,
        onOpenDetail = { detailRoute = it },
        onDismissDetail = { detailRoute = null },
        onOpenAppSwitcher = { showAppSwitcher = true },
    )

    if (showAppSwitcher) {
        AppSwitcherSheet(onDismiss = { showAppSwitcher = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabsScaffold(
    pagerState: PagerState,
    resolver: FeatureFlagResolver,
    crashReporter: CrashReporter,
    calculatorViewModel: CalculatorViewModel,
    planNavController: NavHostController,
    detailRoute: DetailRoute?,
    settingsRepository: SettingsRepository,
    consentRepository: ConsentRepository,
    trackerAccountRepository: TrackerAccountRepository,
    sessionStore: SessionStore,
    onClearHistory: () -> Unit,
    onOpenDetail: (DetailRoute) -> Unit,
    onDismissDetail: () -> Unit,
    onOpenAppSwitcher: () -> Unit,
) {
    val tabs = TabKey.entries
    val coroutineScope = rememberCoroutineScope()

    // Hoisted here (not inside CalculatorScreen) so the Calc-tab title bar below — a sibling of
    // the pager content in this same Scaffold, not a descendant of CalculatorScreen — can open
    // the history screen (§6.3's "Title bar" delta).
    var isCalcHistoryVisible by remember { mutableStateOf(false) }

    val calcResult by calculatorViewModel.result.collectAsStateWithLifecycle()
    val calcInputText by calculatorViewModel.inputState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val colors = LocalDhruvNextColors.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (detailRoute == null) {
                TopAppBar(
                    title = { DhruvWordmarkImage(height = 26.dp) },
                    actions = {
                        if (tabs[pagerState.currentPage] == TabKey.CALC) {
                            IconButton(onClick = { isCalcHistoryVisible = true }) {
                                Icon(Icons.Default.History, contentDescription = "History")
                            }
                            IconButton(
                                onClick = { copyResultToClipboard(calcResult, calcInputText.text, clipboardManager, context) },
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy result")
                            }
                        }
                        IconButton(onClick = onOpenAppSwitcher) {
                            Icon(Icons.Default.Apps, contentDescription = "Switch app")
                        }
                        IconButton(
                            onClick = { onOpenDetail(DetailRoute.Settings) },
                            modifier = Modifier.testTag("top_bar_settings_button"),
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Open Settings")
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = colors.surf,
                            actionIconContentColor = colors.tx2,
                        ),
                )
            }
        },
        bottomBar = {
            BottomBar(
                tabs = tabs.map { it.toBottomBarTab() },
                selectedKey = tabs[pagerState.currentPage].name.lowercase(),
                onTabSelected = { key ->
                    coroutineScope.launch { pagerState.scrollToPage(tabs.indexOf(TabKey.valueOf(key.uppercase()))) }
                },
                modifier = Modifier.testTag("app_navigation_bar"),
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (detailRoute != null) {
                DetailRouteContent(
                    route = detailRoute,
                    resolver = resolver,
                    crashReporter = crashReporter,
                    settingsRepository = settingsRepository,
                    consentRepository = consentRepository,
                    trackerAccountRepository = trackerAccountRepository,
                    sessionStore = sessionStore,
                    onClearHistory = onClearHistory,
                    onBack = onDismissDetail,
                )
            } else {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = false,
                    modifier = Modifier.fillMaxSize().testTag("app_horizontal_pager"),
                ) { page ->
                    when (tabs[page]) {
                        TabKey.HOME -> DashboardScreen()
                        TabKey.MONEY ->
                            NotConfiguredCard(
                                message = "Money lands once the ledger ships",
                                modifier = Modifier.padding(24.dp),
                            )
                        TabKey.CALC ->
                            CalcTab(
                                calculatorViewModel = calculatorViewModel,
                                resolver = resolver,
                                crashReporter = crashReporter,
                                onOpenDetail = onOpenDetail,
                                isHistoryVisible = isCalcHistoryVisible,
                                onHistoryVisibleChange = { isCalcHistoryVisible = it },
                            )
                        TabKey.PLAN ->
                            PlanTab(
                                navController = planNavController,
                                resolver = resolver,
                                crashReporter = crashReporter,
                            )
                        TabKey.INSIGHTS ->
                            NotConfiguredCard(
                                message = "Insights lands once expense tracking ships",
                                modifier = Modifier.padding(24.dp),
                            )
                    }
                }

                if (tabs[pagerState.currentPage] != TabKey.CALC && resolver.isEnabled("assistant")) {
                    AskPill(
                        onClick = { onOpenDetail(DetailRoute.Ask) },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                    )
                }
            }
        }
    }
}

private fun TabKey.toBottomBarTab(): BottomBarTab =
    com.dhruv.core.navigation.BottomNavItems[this]
        ?: error("Unknown tab: $this")

@Composable
private fun CalcTab(
    calculatorViewModel: CalculatorViewModel,
    resolver: FeatureFlagResolver,
    crashReporter: CrashReporter,
    onOpenDetail: (DetailRoute) -> Unit,
    isHistoryVisible: Boolean,
    onHistoryVisibleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        val error by calculatorViewModel.featureError.collectAsStateWithLifecycle()
        FeatureHost("calculator", resolver.isEnabled("calculator"), error, crashReporter) {
            CalculatorScreen(
                viewModel = calculatorViewModel,
                onOpenCurrency = { onOpenDetail(DetailRoute.Currency) },
                onOpenUnit = { onOpenDetail(DetailRoute.UnitConverter) },
                isHistoryVisible = isHistoryVisible,
                onHistoryVisibleChange = onHistoryVisibleChange,
            )
        }
    }
}

private const val PLAN_HOME_ROUTE = "planHome"

private fun PlanTool.route(): String =
    when (this) {
        PlanTool.LOAN -> "loan"
        PlanTool.INVEST -> "invest"
        PlanTool.TAX -> "tax"
        PlanTool.EVERYDAY -> "everyday"
    }

@Composable
private fun PlanTab(
    navController: NavHostController,
    resolver: FeatureFlagResolver,
    crashReporter: CrashReporter,
    modifier: Modifier = Modifier,
) {
    NavHost(navController = navController, startDestination = PLAN_HOME_ROUTE, modifier = modifier.fillMaxSize()) {
        composable(PLAN_HOME_ROUTE) {
            PlanLauncher(onOpenTool = { tool -> navController.navigate(tool.route()) })
        }
        composable(PlanTool.LOAN.route()) {
            val vm: LoansViewModel = koinViewModel()
            val error by vm.featureError.collectAsStateWithLifecycle()
            FeatureHost("loans", resolver.isEnabled("loans"), error, crashReporter) { LoansScreen(viewModel = vm) }
        }
        composable(PlanTool.INVEST.route()) {
            val vm: InvestmentsViewModel = koinViewModel()
            val error by vm.featureError.collectAsStateWithLifecycle()
            FeatureHost("investments", resolver.isEnabled("investments"), error, crashReporter) {
                InvestmentsScreen(viewModel = vm)
            }
        }
        composable(PlanTool.TAX.route()) {
            val vm: TaxViewModel = koinViewModel()
            val error by vm.featureError.collectAsStateWithLifecycle()
            FeatureHost("tax", resolver.isEnabled("tax"), error, crashReporter) { TaxScreen(viewModel = vm) }
        }
        composable(PlanTool.EVERYDAY.route()) {
            val vm: EverydayViewModel = koinViewModel()
            val error by vm.featureError.collectAsStateWithLifecycle()
            FeatureHost("everyday", resolver.isEnabled("everyday"), error, crashReporter) {
                EverydayScreen(viewModel = vm)
            }
        }
    }
}

@Composable
private fun DetailRouteContent(
    route: DetailRoute,
    resolver: FeatureFlagResolver,
    crashReporter: CrashReporter,
    settingsRepository: SettingsRepository,
    consentRepository: ConsentRepository,
    trackerAccountRepository: TrackerAccountRepository,
    sessionStore: SessionStore,
    onClearHistory: () -> Unit,
    onBack: () -> Unit,
) {
    when (route) {
        DetailRoute.Settings ->
            SettingsDetailContent(
                settingsRepository = settingsRepository,
                consentRepository = consentRepository,
                trackerAccountRepository = trackerAccountRepository,
                sessionStore = sessionStore,
                onClearHistory = onClearHistory,
                onBack = onBack,
            )
        DetailRoute.Ask -> AskDetailContent(resolver = resolver, crashReporter = crashReporter, onBack = onBack)
        DetailRoute.Currency -> CurrencyDetailContent(resolver = resolver, crashReporter = crashReporter, onBack = onBack)
        DetailRoute.UnitConverter -> UnitDetailContent(resolver = resolver, crashReporter = crashReporter, onBack = onBack)
        DetailRoute.DateTool -> DateDetailContent(resolver = resolver, crashReporter = crashReporter, onBack = onBack)
        DetailRoute.TimeTool -> TimeDetailContent(resolver = resolver, crashReporter = crashReporter, onBack = onBack)
        DetailRoute.Profile -> ProfileScreen(onBack = onBack)
        DetailRoute.Notifications -> NotifScreen(onBack = onBack)
    }
}
