package com.dhruv.finance.networth

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dhruv.core.flags.FeatureFlagResolver
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.ui.FeatureHost
import com.dhruv.finance.data.tracker.model.Sector
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private const val ROUTE_OVERVIEW = "overview"
private const val ROUTE_ASSETS = "assets/{sector}"
private const val ROUTE_ADD_HOLDING = "addHolding"
private const val ROUTE_HOLDING_DETAIL = "holding/{holdingId}"
private const val ARG_SECTOR = "sector"
private const val ARG_HOLDING_ID = "holdingId"

/**
 * Owns C1-C7's intra-module navigation. `NavTarget` (`libs/core/.../navigation/NavTarget.kt`) is
 * deliberately NOT used for C1->C2/C4 — its own doc comment scopes it to cross-feature/cross-tab
 * dispatch, and every existing intra-module drill-down in this codebase (Plan's tool NavHost in
 * `MainActivity.kt`) uses a local `NavHostController`, not `NavTarget`. This composable is this
 * module's equivalent, self-contained rather than living in the app module, since Home's own tab
 * composable doesn't own a NavHost yet (Phase 7 wires this in from Home with a single call once
 * the real Home screen replaces its current placeholder).
 */
@Composable
fun NetWorthFeatureRoot(modifier: Modifier = Modifier) {
    val resolver: FeatureFlagResolver = koinInject()
    val crashReporter: CrashReporter = koinInject()
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ROUTE_OVERVIEW, modifier = modifier.fillMaxSize()) {
        composable(ROUTE_OVERVIEW) {
            val vm: NetWorthOverviewViewModel = koinViewModel()
            val error by vm.featureError.collectAsStateWithLifecycle()
            FeatureHost("networth", resolver.isEnabled("networth"), error, crashReporter) {
                NetWorthOverviewScreen(
                    viewModel = vm,
                    onOpenSector = { _, sector -> navController.navigate("assets/${sector.name}") },
                    onAddHolding = { navController.navigate(ROUTE_ADD_HOLDING) },
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
            LaunchedEffect(holdingId) { vm.load(holdingId) }
            val error by vm.featureError.collectAsStateWithLifecycle()
            FeatureHost("networth", resolver.isEnabled("networth"), error, crashReporter) {
                HoldingDetailScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    // C5 (AddValuationSheet) doesn't exist until Phase 5 (User Story 3) —
                    // no-op placeholder, not silently omitted (see HoldingDetailScreen's KDoc).
                    onUpdateValue = { },
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
    }
}
