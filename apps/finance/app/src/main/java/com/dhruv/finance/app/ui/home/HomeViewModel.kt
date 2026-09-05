package com.dhruv.finance.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhruv.core.navigation.TabKey
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.PerformanceTracer
import com.dhruv.finance.data.tracker.auth.ConsentRepository
import com.dhruv.finance.data.tracker.auth.ConsentState
import com.dhruv.finance.data.tracker.auth.SessionState
import com.dhruv.finance.data.tracker.auth.SessionStore
import com.dhruv.finance.data.tracker.model.HoldingKind
import com.dhruv.finance.data.tracker.model.HoldingWithValue
import com.dhruv.finance.data.tracker.model.LiabilityMeta
import com.dhruv.finance.data.tracker.model.NetWorthHistoryPoint
import com.dhruv.finance.data.tracker.repo.HoldingRepository
import com.dhruv.finance.data.tracker.repo.LiabilityRepository
import com.dhruv.finance.data.tracker.repo.NetWorthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val BASIS_POINTS_PER_UNIT = 10_000L

/**
 * 01 — Home: net-worth-at-a-glance (spec.md Story 5, HOM-UI-001/003). Shell-owned, not
 * `:feature:networth` — same HOM/PLN correction the module-standard doc already applies to Plan's
 * root screen (E1): Home has no feature module and no feature flag to gate on, so this extends
 * plain `ViewModel()` (matching `SettingsViewModel`), not `FeatureViewModel`.
 *
 * Registered in Koin via [com.dhruv.finance.app.di.appModule].
 */
class HomeViewModel(
    private val netWorthRepository: NetWorthRepository,
    private val holdingRepository: HoldingRepository,
    private val liabilityRepository: LiabilityRepository,
    sessionStore: SessionStore,
    consentRepository: ConsentRepository,
    private val crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : ViewModel() {
    data class UpcomingObligation(
        val holdingId: String,
        val name: String,
        val emiPaise: Long,
        val dueDate: LocalDate,
    )

    data class UiState(
        val isLoading: Boolean = true,
        val netPaise: Long? = null,
        val deltaPercentBps: Int? = null,
        val trendValuesPaise: List<Long> = emptyList(),
        val upcoming: List<UpcomingObligation> = emptyList(),
        val errorMessage: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val sessionState: StateFlow<SessionState> = sessionStore.state
    val consentState: StateFlow<ConsentState> = consentRepository.state

    init {
        crashReporter.setModule("home")
        load()
    }

    /** Only [NetWorthRepository.getHistory] failing blocks the hero — [HoldingRepository.list] and
     * [LiabilityRepository.listAll] failing just yields an empty UPCOMING list (a secondary,
     * non-blocking surface on this screen, same tolerance
     * `LiabilitiesViewModel`'s own merge already applies to a missing `LiabilityMeta` row). */
    fun load() {
        performanceTracer.trace("home_load") { Unit }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val historyResult = netWorthRepository.getHistory()
            val history = historyResult.getOrNull()
            if (history == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = historyResult.exceptionOrNull()?.message ?: "Couldn't load your net worth.",
                    )
                }
                return@launch
            }

            val liabilityHoldings = holdingRepository.list(HoldingKind.LIABILITY).getOrDefault(emptyList())
            val liabilityMetas = liabilityRepository.listAll().getOrDefault(emptyList())

            _uiState.update {
                it.copy(
                    isLoading = false,
                    netPaise = history.lastOrNull()?.netPaise,
                    deltaPercentBps = deltaBps(history),
                    trendValuesPaise = history.map { point -> point.netPaise },
                    upcoming = upcomingObligations(liabilityHoldings, liabilityMetas),
                )
            }
        }
    }
}

/** EMI-only this phase (HOM-UI-003, implementation-plan Phase 2/3 scoped-dependency note — card-bill
 * rows wait for Phase 3's `accounts` table): a liability contributes a row only when it has both an
 * EMI amount and a debit day recorded. Sorted soonest-due-first. */
internal fun upcomingObligations(
    holdings: List<HoldingWithValue>,
    metas: List<LiabilityMeta>,
): List<HomeViewModel.UpcomingObligation> {
    val nameByHoldingId = holdings.associate { it.holding.id to it.holding.name }
    val today = LocalDate.now()
    return metas
        .mapNotNull { meta ->
            val emi = meta.emiPaise ?: return@mapNotNull null
            val debitDay = meta.debitDay ?: return@mapNotNull null
            val name = nameByHoldingId[meta.holdingId] ?: return@mapNotNull null
            HomeViewModel.UpcomingObligation(meta.holdingId, name, emi, nextDueDate(debitDay, today))
        }.sortedBy { it.dueDate }
}

/** Basis-points delta between the newest point and the one ~30 days prior (data-model.md) — at
 * `v_net_worth_history`'s month-end granularity, simply the second-to-last point. Null with fewer
 * than two points, or when the prior point was zero (division by zero). */
internal fun deltaBps(history: List<NetWorthHistoryPoint>): Int? {
    if (history.size < 2) return null
    val latest = history.last()
    val prior = history[history.size - 2]
    return prior.netPaise.takeIf { it != 0L }?.let { p -> ((latest.netPaise - prior.netPaise) * BASIS_POINTS_PER_UNIT / p).toInt() }
}

/** The next occurrence of [debitDay] on/after [today] — this month if not yet passed, else next
 * month. Clamped to the shorter month's last day when [debitDay] exceeds it (e.g. 31 in a 30-day
 * month), the same defensive clamp any day-of-month arithmetic needs. */
internal fun nextDueDate(
    debitDay: Int,
    today: LocalDate,
): LocalDate {
    val thisMonthDue = today.withDayOfMonth(debitDay.coerceAtMost(today.lengthOfMonth()))
    if (!thisMonthDue.isBefore(today)) return thisMonthDue
    val nextMonth = today.plusMonths(1)
    return nextMonth.withDayOfMonth(debitDay.coerceAtMost(nextMonth.lengthOfMonth()))
}

/** HOM-UI-001's greeting, by hour of day (0-23). A pure function so it's testable without a clock
 * dependency threaded through the ViewModel. */
internal fun greetingForHour(hour: Int): String =
    when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }

/** First token of [displayName] (Google profile claim, `SessionState.Active.displayName` — absent
 * for signed-out state or an account with no public name), or null when unavailable. First token
 * only: `displayName` may be a full "First Last" name and the header has no room for both.
 * Blank/whitespace-only names are treated as absent. A pure function so `HomeHeader` can render it
 * in the user's own selected accent color (`LocalDhruvNextColors.current.acc`, ADR-0024 §2) without
 * a hardcoded color ever touching this string. */
internal fun firstNameFrom(displayName: String?): String? = displayName?.trim()?.substringBefore(' ')?.takeIf { it.isNotBlank() }

/** HOM-UI-004/ADR-0024 decision 4: the Ask pill renders on Home/Plan/Insights, never Calc/Money.
 * A pure function (used by `MainActivity.kt`'s pager) so this rule has one definition and one test,
 * instead of being re-derived inline where it's consumed. */
internal fun shouldShowAskPill(tab: TabKey): Boolean = tab == TabKey.HOME || tab == TabKey.PLAN || tab == TabKey.INSIGHTS
