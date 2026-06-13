package com.example.ui.time

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TimeUiState())
    val uiState: StateFlow<TimeUiState> = _uiState.asStateFlow()

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }
}

data class TimeUiState(
    val selectedTab: Int = 0 // 0: Stopwatch, 1: Timer, 2: Alarm
)
