package com.example.ui.time.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AlarmDao
import com.example.data.AlarmEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmViewModel(private val alarmDao: AlarmDao) : ViewModel() {
    val alarms: StateFlow<List<AlarmEntity>> = alarmDao.getAllAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addAlarm(timeInMillis: Long, label: String, puzzleDifficulty: Int) {
        viewModelScope.launch {
            alarmDao.insertAlarm(
                AlarmEntity(
                    timeInMillis = timeInMillis,
                    label = label,
                    isEnabled = true,
                    puzzleDifficulty = puzzleDifficulty
                )
            )
        }
    }

    fun toggleAlarm(alarm: AlarmEntity, isEnabled: Boolean) {
        viewModelScope.launch {
            alarmDao.updateAlarm(alarm.copy(isEnabled = isEnabled))
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            alarmDao.deleteAlarm(alarm)
        }
    }
}
