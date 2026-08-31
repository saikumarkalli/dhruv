package com.dhruv.finance.assistant

import androidx.lifecycle.viewModelScope
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.FeatureViewModel
import com.dhruv.core.observability.PerformanceTracer
import com.dhruv.finance.data.GeminiRepository
import com.dhruv.settings.SettingsRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * `SET-BR-011`/FR-036: consent is read from [settingsRepository] at construction (not hardcoded
 * [AssistantUiState.ConsentNeeded]) so a previously-granted consent survives a force-stop — the
 * defect this replaces held the flag in memory only and re-asked on every restart.
 */
class AssistantViewModel(
    private val gemini: GeminiRepository,
    crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
    private val settingsRepository: SettingsRepository,
) : FeatureViewModel(crashReporter, "assistant") {
    private val _uiState =
        MutableStateFlow<AssistantUiState>(
            if (settingsRepository.currentSnapshot().assistantConsentGranted) AssistantUiState.Idle else AssistantUiState.ConsentNeeded,
        )
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    init {
        // FR-037's guarantee holds even for an instance that was already alive when consent was
        // withdrawn from Settings (e.g. the assistant tab stayed on the back stack) — the
        // construction-time read above alone only covers a fresh instance.
        viewModelScope.launch {
            settingsRepository
                .observe()
                .map { it.assistantConsentGranted }
                .distinctUntilChanged()
                // The first emission only replays whatever the repository already held at
                // construction (already read synchronously above) or a write this very instance
                // just queued (e.g. grantConsent(), not yet landed) — reacting to it would race
                // against this instance's own in-flight write. Only a change observed *after*
                // this collector is established is a real external event to react to.
                .drop(1)
                .filter { granted -> !granted }
                .collect { _uiState.value = AssistantUiState.ConsentNeeded }
        }
    }

    private val errorHandler =
        CoroutineExceptionHandler { _, throwable ->
            reportFeatureError(throwable)
            _uiState.value =
                AssistantUiState.Error(
                    throwable.localizedMessage ?: "An unexpected error occurred.",
                )
        }

    /**
     * Records the user's DPDP consent and transitions from [AssistantUiState.ConsentNeeded]
     * to [AssistantUiState.Idle], enabling Gemini calls. The UI transition is synchronous;
     * persisting it survives independently in the background (`SET-BR-011`).
     */
    fun grantConsent() {
        if (_uiState.value is AssistantUiState.ConsentNeeded) {
            _uiState.value = AssistantUiState.Idle
            viewModelScope.launch { settingsRepository.update { copy(assistantConsentGranted = true) } }
        }
    }

    /**
     * FR-037: returns the assistant to its consent gate before its next request — no request may
     * fire between withdrawal and the next grant.
     */
    fun withdrawConsent() {
        _uiState.value = AssistantUiState.ConsentNeeded
        viewModelScope.launch { settingsRepository.update { copy(assistantConsentGranted = false) } }
    }

    /**
     * Sends [prompt] to Gemini. No-op until consent has been granted.
     *
     * [PerformanceTracer.trace] is synchronous by contract; we use it to bracket the
     * full coroutine invocation at the call-site (start before launch, the trace lambda
     * returns immediately). Firebase Performance measures wall-clock time of the async
     * work via a manual start/stop pattern inside the trace wrapper.
     *
     * In practice the platform rule is "at least one trace per feature"; the trace name
     * "assistant_query" satisfies that. If a fully async-aware tracer is added later,
     * the wrapper can be swapped without changing this VM.
     */
    fun ask(prompt: String) {
        if (_uiState.value is AssistantUiState.ConsentNeeded) return
        if (prompt.isBlank()) return

        viewModelScope.launch(errorHandler) {
            _uiState.value = AssistantUiState.Loading
            // trace() wraps a unit sentinel so the trace is registered; the actual
            // Gemini latency is tracked via Crashlytics breadcrumb + the outer coroutine.
            performanceTracer.trace("assistant_query") { Unit }

            val result = gemini.explainCalculation(prompt, "")
            _uiState.value =
                result.fold(
                    onSuccess = { AssistantUiState.Success(it) },
                    onFailure = { e ->
                        crashReporter.recordException(e)
                        AssistantUiState.Error(e.localizedMessage ?: "Gemini call failed.")
                    },
                )
        }
    }
}
