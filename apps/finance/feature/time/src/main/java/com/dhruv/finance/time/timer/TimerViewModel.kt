package com.dhruv.finance.time.timer

import androidx.lifecycle.viewModelScope
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.FeatureViewModel
import com.dhruv.core.observability.PerformanceTracer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TimerState(
    val initialTimeMs: Long = 60000L,
    val remainingTimeMs: Long = 60000L,
    val isRunning: Boolean = false,
    val isInputMode: Boolean = true,
    val inputString: String = "",
) {
    val progress: Float
        get() = if (initialTimeMs > 0) remainingTimeMs.toFloat() / initialTimeMs.toFloat() else 0f
}

class TimerViewModel(
    crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : FeatureViewModel(crashReporter, "timer") {
    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private var timerJob: Job? = null

    fun updateInput(digit: String) {
        val currentInput = _state.value.inputString
        if (currentInput.length < 6) {
            _state.value = _state.value.copy(inputString = currentInput + digit)
        }
    }

    fun clearInput() {
        _state.value = _state.value.copy(inputString = "")
    }

    fun setPreset(minutes: Int) {
        val ms = minutes * 60000L
        _state.value =
            _state.value.copy(
                initialTimeMs = ms,
                remainingTimeMs = ms,
                isInputMode = false,
            )
    }

    fun startTimer() {
        if (_state.value.isInputMode) {
            val input = _state.value.inputString.padStart(6, '0')
            val h = input.substring(0, 2).toLong()
            val m = input.substring(2, 4).toLong()
            val s = input.substring(4, 6).toLong()
            val ms = (h * 3600 + m * 60 + s) * 1000
            if (ms > 0) {
                _state.value =
                    _state.value.copy(
                        initialTimeMs = ms,
                        remainingTimeMs = ms,
                        isInputMode = false,
                    )
            } else {
                return // Invalid time
            }
        }

        performanceTracer.trace("time_timer_start") {
            _state.value = _state.value.copy(isRunning = true)
        }
        timerJob?.cancel()
        timerJob =
            viewModelScope.launch(exceptionHandler) {
                while (_state.value.remainingTimeMs > 0 && _state.value.isRunning) {
                    delay(50)
                    _state.value =
                        _state.value.copy(
                            remainingTimeMs = maxOf(0, _state.value.remainingTimeMs - 50),
                        )
                    if (_state.value.remainingTimeMs <= 0) {
                        _state.value = _state.value.copy(isRunning = false)
                    }
                }
            }
    }

    fun pauseTimer() {
        _state.value = _state.value.copy(isRunning = false)
        timerJob?.cancel()
    }

    fun resetTimer() {
        _state.value =
            _state.value.copy(
                isRunning = false,
                isInputMode = true,
                inputString = "",
                remainingTimeMs = 60000L,
                initialTimeMs = 60000L,
            )
        timerJob?.cancel()
    }
}
