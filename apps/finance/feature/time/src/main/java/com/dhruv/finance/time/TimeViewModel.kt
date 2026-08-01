package com.dhruv.finance.time

import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.FeatureViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimeViewModel(
    crashReporter: CrashReporter,
) : FeatureViewModel(crashReporter, "time") {
    private val _uiState = MutableStateFlow(TimeUiState())
    val uiState: StateFlow<TimeUiState> = _uiState.asStateFlow()

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }
}

data class TimeUiState(
    val selectedTab: Int = 0, // 0: Stopwatch, 1: Timer
)
