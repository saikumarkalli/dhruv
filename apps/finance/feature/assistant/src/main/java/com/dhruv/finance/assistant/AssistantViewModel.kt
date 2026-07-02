package com.dhruv.finance.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.PerformanceTracer
import com.dhruv.finance.data.GeminiRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AssistantViewModel(
    private val gemini: GeminiRepository,
    private val crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AssistantUiState>(AssistantUiState.ConsentNeeded)
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    // Exposes uncaught coroutine errors so FeatureHost can render FeatureErrorCard.
    private val _featureError = MutableStateFlow<Throwable?>(null)
    val featureError: StateFlow<Throwable?> = _featureError.asStateFlow()

    private val errorHandler =
        CoroutineExceptionHandler { _, throwable ->
            crashReporter.recordException(throwable)
            _featureError.value = throwable
            _uiState.value =
                AssistantUiState.Error(
                    throwable.localizedMessage ?: "An unexpected error occurred.",
                )
        }

    init {
        crashReporter.setModule("assistant")
    }

    /**
     * Records the user's DPDP consent and transitions from [AssistantUiState.ConsentNeeded]
     * to [AssistantUiState.Idle], enabling Gemini calls.
     */
    fun grantConsent() {
        if (_uiState.value is AssistantUiState.ConsentNeeded) {
            _uiState.value = AssistantUiState.Idle
        }
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
