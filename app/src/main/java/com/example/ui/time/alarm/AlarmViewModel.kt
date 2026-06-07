package com.example.ui.time.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AlarmDao
import com.example.data.AlarmEntity
import com.example.service.alarm.AlarmScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmViewModel(
    private val alarmDao: AlarmDao,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {
    val alarms: StateFlow<List<AlarmEntity>> = alarmDao.getAllAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addAlarm(timeInMillis: Long, label: String, puzzleDifficulty: Int) {
        viewModelScope.launch {
            val alarm = AlarmEntity(
                timeInMillis = timeInMillis,
                label = label,
                isEnabled = true,
                puzzleDifficulty = puzzleDifficulty
            )
            val id = alarmDao.insertAlarm(alarm)
            alarmScheduler.schedule(alarm.copy(id = id))
        }
    }

    fun toggleAlarm(alarm: AlarmEntity, isEnabled: Boolean) {
        viewModelScope.launch {
            alarmDao.updateAlarm(alarm.copy(isEnabled = isEnabled))
            if (isEnabled) {
                alarmScheduler.schedule(alarm)
            } else {
                alarmScheduler.cancel(alarm)
            }
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            alarmDao.deleteAlarm(alarm)
            alarmScheduler.cancel(alarm)
        }
    }
}
