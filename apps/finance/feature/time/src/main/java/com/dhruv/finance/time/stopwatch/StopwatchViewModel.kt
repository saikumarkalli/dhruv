package com.dhruv.finance.time.stopwatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.PerformanceTracer
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LapData(
    val lapNumber: Int,
    val lapTimeMs: Long,
    val totalTimeMs: Long,
    val deltaMs: Long,
)

data class StopwatchState(
    val timeMs: Long = 0L,
    val isRunning: Boolean = false,
    val laps: List<LapData> = emptyList(),
)

class StopwatchViewModel(
    private val crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : ViewModel() {
    private val _state = MutableStateFlow(StopwatchState())
    val state: StateFlow<StopwatchState> = _state.asStateFlow()

    private val _featureError = MutableStateFlow<Throwable?>(null)
    val featureError: StateFlow<Throwable?> = _featureError.asStateFlow()

    private val exceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            crashReporter.recordException(throwable)
            _featureError.value = throwable
        }

    private var timerJob: Job? = null
    private var startTime = 0L
    private var lastLapTime = 0L

    fun toggleStartStop() {
        if (_state.value.isRunning) {
            timerJob?.cancel()
            _state.value = _state.value.copy(isRunning = false)
        } else {
            performanceTracer.trace("time_stopwatch_start") {
                startTime = System.currentTimeMillis() - _state.value.timeMs
                _state.value = _state.value.copy(isRunning = true)
            }
            timerJob =
                viewModelScope.launch(exceptionHandler) {
                    while (true) {
                        _state.value = _state.value.copy(timeMs = System.currentTimeMillis() - startTime)
                        delay(10) // 10ms update rate
                    }
                }
        }
    }

    fun lapOrReset() {
        if (_state.value.isRunning) {
            // Lap
            val currentMs = _state.value.timeMs
            val lapTime = currentMs - lastLapTime
            val prevLapTime =
                _state.value.laps
                    .firstOrNull()
                    ?.lapTimeMs ?: 0L
            val delta = if (_state.value.laps.isEmpty()) 0L else lapTime - prevLapTime

            val newLap =
                LapData(
                    lapNumber = _state.value.laps.size + 1,
                    lapTimeMs = lapTime,
                    totalTimeMs = currentMs,
                    deltaMs = delta,
                )
            _state.value = _state.value.copy(laps = listOf(newLap) + _state.value.laps)
            lastLapTime = currentMs
        } else {
            // Reset
            timerJob?.cancel()
            _state.value = StopwatchState()
            lastLapTime = 0L
        }
    }
}
