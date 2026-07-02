package com.dhruv.finance.time

import androidx.lifecycle.ViewModel
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.PerformanceTracer
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimeViewModel(
    private val crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TimeUiState())
    val uiState: StateFlow<TimeUiState> = _uiState.asStateFlow()

    private val _featureError = MutableStateFlow<Throwable?>(null)
    val featureError: StateFlow<Throwable?> = _featureError.asStateFlow()

    private val exceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            crashReporter.recordException(throwable)
            _featureError.value = throwable
        }

    init {
        crashReporter.setModule("time")
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }
}

data class TimeUiState(
    val selectedTab: Int = 0, // 0: Stopwatch, 1: Timer
)
