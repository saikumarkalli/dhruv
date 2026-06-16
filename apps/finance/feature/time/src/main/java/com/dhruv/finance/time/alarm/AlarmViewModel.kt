package com.dhruv.finance.time.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.PerformanceTracer
import com.dhruv.finance.data.AlarmDao
import com.dhruv.finance.data.AlarmEntity
import com.dhruv.finance.time.service.alarm.AlarmScheduler
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmViewModel(
    private val alarmDao: AlarmDao,
    private val alarmScheduler: AlarmScheduler,
    private val crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer
) : ViewModel() {
    val alarms: StateFlow<List<AlarmEntity>> = alarmDao.getAllAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _featureError = MutableStateFlow<Throwable?>(null)
    val featureError: StateFlow<Throwable?> = _featureError.asStateFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        crashReporter.recordException(throwable)
        _featureError.value = throwable
    }

    init {
        crashReporter.setModule("time")
    }

    fun addAlarm(timeInMillis: Long, label: String, puzzleDifficulty: Int) {
        viewModelScope.launch(exceptionHandler) {
            val alarm = performanceTracer.trace("time_alarm_schedule") {
                AlarmEntity(
                    timeInMillis = timeInMillis,
                    label = label,
                    isEnabled = true,
                    puzzleDifficulty = puzzleDifficulty
                )
            }
            val id = alarmDao.insertAlarm(alarm)
            alarmScheduler.schedule(alarm.copy(id = id))
        }
    }

    fun toggleAlarm(alarm: AlarmEntity, isEnabled: Boolean) {
        viewModelScope.launch(exceptionHandler) {
            alarmDao.updateAlarm(alarm.copy(isEnabled = isEnabled))
            if (isEnabled) {
                alarmScheduler.schedule(alarm)
            } else {
                alarmScheduler.cancel(alarm)
            }
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch(exceptionHandler) {
            alarmDao.deleteAlarm(alarm)
            alarmScheduler.cancel(alarm)
        }
    }
}
