package com.dhruv.finance.networth

import androidx.lifecycle.viewModelScope
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.FeatureViewModel
import com.dhruv.core.observability.PerformanceTracer
import com.dhruv.finance.data.tracker.auth.ConsentRepository
import com.dhruv.finance.data.tracker.auth.ConsentState
import com.dhruv.finance.data.tracker.auth.SessionState
import com.dhruv.finance.data.tracker.auth.SessionStore
import com.dhruv.finance.data.tracker.model.HoldingKind
import com.dhruv.finance.data.tracker.model.HoldingWithValue
import com.dhruv.finance.data.tracker.model.Sector
import com.dhruv.finance.data.tracker.repo.HoldingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** C2 — sector-grouped assets list. [sessionState]/[consentState] (NW-UI-005, added Phase 9 — this
 * screen previously had no signed-out/consent gating at all, found during the Phase 8 QA pass)
 * mirror [NetWorthOverviewViewModel]'s exact pattern. */
class AssetsViewModel(
    private val holdingRepository: HoldingRepository,
    sessionStore: SessionStore,
    consentRepository: ConsentRepository,
    crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : FeatureViewModel(crashReporter, "networth") {
    data class UiState(
        val isLoading: Boolean = true,
        val holdings: List<HoldingWithValue> = emptyList(),
        val selectedSectorFilter: Sector? = null,
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
        performanceTracer.trace("networth_assets_load") { Unit }
        viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            holdingRepository
                .list(HoldingKind.ASSET)
                .onSuccess { list -> _uiState.update { it.copy(isLoading = false, holdings = list) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Couldn't load your assets.") }
                }
        }
    }

    fun setSectorFilter(sector: Sector?) {
        _uiState.update { it.copy(selectedSectorFilter = sector) }
    }
}
