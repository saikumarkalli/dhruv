package com.dhruv.finance.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhruv.core.navigation.NavTarget
import com.dhruv.core.navigation.NavigationDispatcher
import com.dhruv.core.navigation.PlanTool
import com.dhruv.core.ui.components.EmptyStateCard
import com.dhruv.core.ui.components.MoneyText
import com.dhruv.core.ui.components.MoneyTextVariant
import com.dhruv.core.ui.components.NxButton
import com.dhruv.core.ui.components.NxButtonVariant
import com.dhruv.core.ui.components.NxCard
import com.dhruv.core.ui.components.OfflineStateCard
import com.dhruv.core.ui.components.QuickActionTile
import com.dhruv.core.ui.components.SectionLabel
import com.dhruv.core.ui.components.SignedOutCard
import com.dhruv.core.ui.components.SkeletonBlock
import com.dhruv.core.ui.components.StatDeltaChip
import com.dhruv.core.ui.components.TrendSparkline
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import com.dhruv.finance.app.R
import com.dhruv.finance.app.ui.shell.DetailRoute
import com.dhruv.finance.data.tracker.auth.SessionState
import org.koin.compose.koinInject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val DATE_LINE_FORMAT = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.US)
private val DUE_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM", Locale.US)

/**
 * 01 — Home (spec.md Story 5). [onOpenDetail] is the existing shell-level mechanism for the
 * Currency quick action — `NavTarget`'s own doc comment deliberately excludes Currency/Unit/Date/
 * Time/Settings/Ask (they're shell detail routes, not tab-scoped), so Currency cannot go through
 * `NavigationDispatcher` the way Loan EMI/SIP/GST do. This is the same split `CalcTab` already uses
 * for its own currency/unit shortcuts (`MainActivity.kt`), not a new mechanism.
 *
 * `NxHomeTopBar` (`:libs:core`) exists but is deliberately NOT used here: it has no app-switcher
 * icon, and swapping it in as this tab's top bar would silently drop N5's "app-switcher reachable
 * from every tab's top bar" guarantee, which the shared `TopAppBar` in `MainActivity.kt` already
 * satisfies for every tab including this one. The greeting/date line render as an in-content header
 * instead — same pattern every other screen in this app uses for its own screen-specific heading.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenDetail: (DetailRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigationDispatcher: NavigationDispatcher = koinInject()
    val colors = LocalDhruvNextColors.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val consentState by viewModel.consentState.collectAsStateWithLifecycle()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.bg)
                .verticalScroll(rememberScrollState())
                .padding(DhruvNextSpacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
    ) {
        HomeHeader(displayName = (sessionState as? SessionState.Active)?.displayName)

        when {
            sessionState !is SessionState.Active ->
                SignedOutCard(
                    message = "Sign in to see your net worth.",
                    actionLabel = "Go to Account",
                    onAction = { onOpenDetail(DetailRoute.Settings) },
                )
            !consentState.syncFinancialRecords ->
                SignedOutCard(
                    message = "Turn on “Sync my financial records” in Settings to use the tracker.",
                    actionLabel = "Go to Settings",
                    onAction = { onOpenDetail(DetailRoute.Settings) },
                )
            uiState.isLoading && uiState.netPaise == null -> SkeletonBlock(height = 220.dp)
            uiState.errorMessage != null && uiState.netPaise == null ->
                OfflineStateCard(
                    message = uiState.errorMessage ?: "Couldn't load your net worth.",
                    onRetry = viewModel::load,
                )
            else -> {
                NetWorthHero(uiState, upcomingCount = uiState.upcoming.size)
                NxButton(
                    text = "View details",
                    onClick = { onOpenDetail(DetailRoute.NetWorth) },
                    variant = NxButtonVariant.Outline,
                    block = true,
                )
                QuickActionsRow(
                    onOpenLoanEmi = { navigationDispatcher.navigate(NavTarget.OpenPlanTool(PlanTool.LOAN)) },
                    onOpenSip = { navigationDispatcher.navigate(NavTarget.OpenPlanTool(PlanTool.INVEST)) },
                    onOpenCurrency = { onOpenDetail(DetailRoute.Currency) },
                    onOpenGst = { navigationDispatcher.navigate(NavTarget.OpenPlanTool(PlanTool.TAX)) },
                )
                UpcomingSection(uiState.upcoming)
            }
        }
    }
}

@Composable
private fun HomeHeader(
    displayName: String?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    val greeting = greetingWithName(greetingForHour(LocalTime.now().hour), displayName)
    val dateLine = LocalDate.now().format(DATE_LINE_FORMAT)
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = greeting, color = colors.tx, fontSize = DhruvNextType.title, fontWeight = FontWeight.Bold)
        Text(text = dateLine, color = colors.tx3, fontSize = DhruvNextType.meta)
    }
}

@Composable
private fun NetWorthHero(
    uiState: HomeViewModel.UiState,
    upcomingCount: Int,
) {
    NxCard {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            // Phase 10, T074: the design's Home (01) hero carries a one-line status ("everything
            // on track") this card never had — derived from the same `upcoming` list the section
            // below already renders, not a new data source.
            Text(
                text =
                    if (upcomingCount == 0) {
                        stringResource(R.string.home_status_on_track)
                    } else {
                        stringResource(R.string.home_status_upcoming_count, upcomingCount)
                    },
                color = LocalDhruvNextColors.current.tx3,
                fontSize = DhruvNextType.meta,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            MoneyText(paise = uiState.netPaise ?: 0L, variant = MoneyTextVariant.Hero)
            uiState.deltaPercentBps?.let { bps ->
                Spacer(Modifier.height(DhruvNextSpacing.inputGroupGap))
                val deltaPercent = bps / 100.0
                StatDeltaChip(
                    text = "%.1f%% this month".format(Locale.US, abs(deltaPercent)),
                    isPositive = deltaPercent >= 0,
                )
            }
            if (uiState.trendValuesPaise.size > 1) {
                Spacer(Modifier.height(DhruvNextSpacing.interCardGap))
                // Phase 10, T069: chart contentDescription — previously absent.
                val trendDescription = stringResource(R.string.home_trend_chart_description)
                TrendSparkline(
                    values = uiState.trendValuesPaise.map { it.toFloat() },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .semantics { contentDescription = trendDescription },
                )
            }
        }
    }
}

@Composable
private fun QuickActionsRow(
    onOpenLoanEmi: () -> Unit,
    onOpenSip: () -> Unit,
    onOpenCurrency: () -> Unit,
    onOpenGst: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        QuickActionTile(label = "Loan EMI", icon = Icons.Default.AccountBalance, onClick = onOpenLoanEmi)
        QuickActionTile(label = "SIP", icon = Icons.AutoMirrored.Filled.TrendingUp, onClick = onOpenSip)
        QuickActionTile(label = "Currency", icon = Icons.Default.SwapHoriz, onClick = onOpenCurrency)
        QuickActionTile(label = "GST", icon = Icons.Default.Receipt, onClick = onOpenGst)
    }
}

@Composable
private fun UpcomingSection(upcoming: List<HomeViewModel.UpcomingObligation>) {
    NxCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            SectionLabel(text = "Upcoming")
            Spacer(Modifier.height(DhruvNextSpacing.interCardGap))
            if (upcoming.isEmpty()) {
                EmptyStateCard(message = "No upcoming EMI payments recorded.")
            } else {
                upcoming.forEachIndexed { index, obligation ->
                    UpcomingRow(obligation)
                    if (index != upcoming.lastIndex) {
                        Spacer(Modifier.height(DhruvNextSpacing.inputGroupGap))
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingRow(obligation: HomeViewModel.UpcomingObligation) {
    val colors = LocalDhruvNextColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(text = obligation.name, color = colors.tx, fontSize = DhruvNextType.body, fontWeight = FontWeight.Medium)
            Text(text = "Due ${obligation.dueDate.format(DUE_DATE_FORMAT)}", color = colors.tx3, fontSize = DhruvNextType.meta)
        }
        MoneyText(paise = obligation.emiPaise, variant = MoneyTextVariant.Inline)
    }
}
