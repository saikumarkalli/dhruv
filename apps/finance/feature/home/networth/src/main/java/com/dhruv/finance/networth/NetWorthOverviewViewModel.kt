package com.dhruv.finance.networth

import androidx.lifecycle.viewModelScope
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.FeatureViewModel
import com.dhruv.core.observability.PerformanceTracer
import com.dhruv.finance.data.tracker.auth.ConsentRepository
import com.dhruv.finance.data.tracker.auth.ConsentState
import com.dhruv.finance.data.tracker.auth.SessionState
import com.dhruv.finance.data.tracker.auth.SessionStore
import com.dhruv.finance.data.tracker.model.NetWorthSummary
import com.dhruv.finance.data.tracker.repo.NetWorthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** C1 — net-worth overview (NW-UI-001/NW-FLOW-001). */
class NetWorthOverviewViewModel(
    private val netWorthRepository: NetWorthRepository,
    sessionStore: SessionStore,
    consentRepository: ConsentRepository,
    crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : FeatureViewModel(crashReporter, "networth") {
    data class UiState(
        val isLoading: Boolean = true,
        val summary: NetWorthSummary? = null,
        val errorMessage: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val sessionState: StateFlow<SessionState> = sessionStore.state
    val consentState: StateFlow<ConsentState> = consentRepository.state

    init {
        load()
    }

    fun load() {
        // Sentinel trace (platform rule: at least one Performance trace per feature) — the sync
        // PerformanceTracer API can't wrap a suspend network call, same accepted pattern as
        // CurrencyViewModel.syncCurrencyRates()/AssistantViewModel.ask().
        performanceTracer.trace("networth_load") { Unit }
        viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            netWorthRepository
                .getSummary()
                .onSuccess { summary -> _uiState.update { it.copy(isLoading = false, summary = summary) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Couldn't load your net worth.") }
                }
        }
    }
}
