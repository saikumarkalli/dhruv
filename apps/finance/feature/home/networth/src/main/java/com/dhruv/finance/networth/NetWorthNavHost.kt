package com.dhruv.finance.networth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dhruv.core.flags.FeatureFlagResolver
import com.dhruv.core.navigation.NavTarget
import com.dhruv.core.navigation.NavigationDispatcher
import com.dhruv.core.navigation.PlanTool
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.ui.FeatureHost
import com.dhruv.core.ui.components.NxTopBar
import com.dhruv.finance.data.tracker.model.HoldingKind
import com.dhruv.finance.data.tracker.model.Sector
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private const val ROUTE_OVERVIEW = "overview"
private const val ROUTE_ASSETS = "assets/{sector}"
private const val ROUTE_ADD_HOLDING = "addHolding"
private const val ROUTE_EDIT_HOLDING = "editHolding/{holdingId}"
private const val ROUTE_HOLDING_DETAIL = "holding/{holdingId}"
private const val ROUTE_ADD_VALUATION = "addValuation/{holdingId}?correcting={correcting}&last={last}"
private const val ROUTE_LIABILITIES = "liabilities"
private const val ROUTE_LIABILITY_DETAIL = "liability/{holdingId}"
private const val ARG_SECTOR = "sector"
private const val ARG_HOLDING_ID = "holdingId"
private const val ARG_CORRECTING = "correcting"
private const val ARG_LAST = "last"

/**
 * Owns C1-C7's intra-module navigation. `NavTarget` (`libs/core/.../navigation/NavTarget.kt`) is
 * deliberately NOT used for C1->C2/C4 — its own doc comment scopes it to cross-feature/cross-tab
 * dispatch, and every existing intra-module drill-down in this codebase (Plan's tool NavHost in
 * `MainActivity.kt`) uses a local `NavHostController`, not `NavTarget`. This composable is this
 * module's equivalent, self-contained rather than living in the app module.
 *
 * T032's exception: C7's prepay hand-off to the Plan tab's loan calculator IS genuinely cross-tab
 * (a different Gradle module), so it goes through the injected [NavigationDispatcher] +
 * `NavTarget.OpenPlanTool`, not the local [rememberNavController] used everywhere else in this file.
 *
 * [navController]/[onExit] (Phase 8): this composable used to create its own throwaway
 * `NavHostController` (nothing outside it could reach C1-C7 at all — the whole module was
 * unmounted dead code until this phase). Home now opens it as a shell-level `DetailRoute`
 * (`MainActivity.kt`), which owns [navController] so it can also integrate this module's nested
 * back stack into the hardware back button — same hoisting `PlanTab`'s `planNavController` already
 * uses. [onExit] is null only in tests/previews that construct this composable standalone; the real
 * app always supplies it, rendering [ROUTE_OVERVIEW] with a back arrow that leaves this section
 * (N2) instead of behaving like a tab root with none (N1) — correct here because this is mounted as
 * a detail route, not a tab.
 */
@Composable
fun NetWorthFeatureRoot(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    onExit: (() -> Unit)? = null,
) {
    val resolver: FeatureFlagResolver = koinInject()
    val crashReporter: CrashReporter = koinInject()
    val navigationDispatcher: NavigationDispatcher = koinInject()

    NavHost(navController = navController, startDestination = ROUTE_OVERVIEW, modifier = modifier.fillMaxSize()) {
        composable(ROUTE_OVERVIEW) {
            val vm: NetWorthOverviewViewModel = koinViewModel()
            LifecycleResumeEffect(Unit) {
                vm.load()
                onPauseOrDispose { }
            }
            val error by vm.featureError.collectAsStateWithLifecycle()
            Column(modifier = Modifier.fillMaxSize()) {
                onExit?.let { NxTopBar(title = "Net worth", onBack = it) }
                FeatureHost("networth", resolver.isEnabled("networth"), error, crashReporter) {
                    NetWorthOverviewScreen(
                        viewModel = vm,
                        onOpenSector = { kind, sector ->
                            if (kind == HoldingKind.LIABILITY) {
                                navController.navigate(ROUTE_LIABILITIES)
                            } else {
                                navController.navigate("assets/${sector.name}")
                            }
                        },
                        onAddHolding = { navController.navigate(ROUTE_ADD_HOLDING) },
                    )
                }
            }
        }
        composable(ROUTE_LIABILITIES) {
            val vm: LiabilitiesViewModel = koinViewModel()
            LifecycleResumeEffect(Unit) {
                vm.load()
                onPauseOrDispose { }
            }
            val error by vm.featureError.collectAsStateWithLifecycle()
            FeatureHost("networth", resolver.isEnabled("networth"), error, crashReporter) {
                LiabilitiesScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onOpenLiability = { holdingId -> navController.navigate("liability/$holdingId") },
                )
            }
        }
        composable(
            route = ROUTE_LIABILITY_DETAIL,
            arguments = listOf(navArgument(ARG_HOLDING_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val vm: LiabilityDetailViewModel = koinViewModel()
            val holdingId = backStackEntry.arguments?.getString(ARG_HOLDING_ID).orEmpty()
            // Resume (not LaunchedEffect) — fires on every return to this screen, including from
            // a prepay hand-off or a future edit flow, not just the first time this route composes.
            LifecycleResumeEffect(holdingId) {
                vm.load(holdingId)
                onPauseOrDispose { }
            }
            val error by vm.featureError.collectAsStateWithLifecycle()
            FeatureHost("networth", resolver.isEnabled("networth"), error, crashReporter) {
                LiabilityDetailScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onOpenLoanCalculator = { navigationDispatcher.navigate(NavTarget.OpenPlanTool(PlanTool.LOAN)) },
                )
            }
        }
        composable(
            route = ROUTE_ASSETS,
            arguments = listOf(navArgument(ARG_SECTOR) { type = NavType.StringType }),
        ) { backStackEntry ->
            val vm: AssetsViewModel = koinViewModel()
            val sectorArg = backStackEntry.arguments?.getString(ARG_SECTOR)
            LaunchedEffect(sectorArg) { vm.setSectorFilter(sectorArg?.let(Sector::fromCode)) }
            LifecycleResumeEffect(Unit) {
                vm.load()
                onPauseOrDispose { }
            }
            val error by vm.featureError.collectAsStateWithLifecycle()
            FeatureHost("networth", resolver.isEnabled("networth"), error, crashReporter) {
                AssetsScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onOpenHolding = { holdingId -> navController.navigate("holding/$holdingId") },
                )
            }
        }
        composable(
            route = ROUTE_HOLDING_DETAIL,
            arguments = listOf(navArgument(ARG_HOLDING_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val vm: HoldingDetailViewModel = koinViewModel()
            val holdingId = backStackEntry.arguments?.getString(ARG_HOLDING_ID).orEmpty()
            // Resume, not LaunchedEffect — NW-FLOW-002 needs this screen to reflect a value just
            // recorded via C5, which returns here via popBackStack (holdingId unchanged, so a
            // LaunchedEffect keyed on it alone would never re-fire).
            LifecycleResumeEffect(holdingId) {
                vm.load(holdingId)
                onPauseOrDispose { }
            }
            val error by vm.featureError.collectAsStateWithLifecycle()
            FeatureHost("networth", resolver.isEnabled("networth"), error, crashReporter) {
                HoldingDetailScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate("editHolding/$holdingId") },
                    onUpdateValue = { lastValuePaise ->
                        navController.navigate("addValuation/$holdingId?last=${lastValuePaise ?: ""}")
                    },
                    onCorrectEntry = { valuationId, valuePaise ->
                        navController.navigate("addValuation/$holdingId?correcting=$valuationId&last=$valuePaise")
                    },
                )
            }
        }
        composable(
            route = ROUTE_ADD_VALUATION,
            arguments =
                listOf(
                    navArgument(ARG_HOLDING_ID) { type = NavType.StringType },
                    navArgument(ARG_CORRECTING) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument(ARG_LAST) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
        ) { backStackEntry ->
            val vm: AddValuationViewModel = koinViewModel()
            val holdingId = backStackEntry.arguments?.getString(ARG_HOLDING_ID).orEmpty()
            val correcting = backStackEntry.arguments?.getString(ARG_CORRECTING)
            val last = backStackEntry.arguments?.getString(ARG_LAST)?.toLongOrNull()
            LaunchedEffect(holdingId, correcting, last) { vm.start(holdingId, last, correcting) }
            val error by vm.featureError.collectAsStateWithLifecycle()
            FeatureHost("networth", resolver.isEnabled("networth"), error, crashReporter) {
                AddValuationSheet(
                    viewModel = vm,
                    onSaved = { navController.popBackStack() },
                    onDismissRequest = { navController.popBackStack() },
                )
            }
        }
        composable(ROUTE_ADD_HOLDING) {
            val vm: AddEditHoldingViewModel = koinViewModel()
            val error by vm.featureError.collectAsStateWithLifecycle()
            FeatureHost("networth", resolver.isEnabled("networth"), error, crashReporter) {
                AddEditHoldingScreen(
                    viewModel = vm,
                    onSaved = { navController.popBackStack(ROUTE_OVERVIEW, inclusive = false) },
                    onClose = { navController.popBackStack() },
                )
            }
        }
        composable(
            route = ROUTE_EDIT_HOLDING,
            arguments = listOf(navArgument(ARG_HOLDING_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val vm: AddEditHoldingViewModel = koinViewModel()
            val holdingId = backStackEntry.arguments?.getString(ARG_HOLDING_ID).orEmpty()
            LaunchedEffect(holdingId) { vm.startEditing(holdingId) }
            val error by vm.featureError.collectAsStateWithLifecycle()
            FeatureHost("networth", resolver.isEnabled("networth"), error, crashReporter) {
                AddEditHoldingScreen(
                    viewModel = vm,
                    onSaved = { navController.popBackStack() },
                    onClose = { navController.popBackStack() },
                )
            }
        }
    }
}
