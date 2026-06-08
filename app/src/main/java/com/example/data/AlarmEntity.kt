package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timeInMillis: Long,
    val label: String,
    val isEnabled: Boolean,
    val puzzleDifficulty: Int // 0: Easy, 1: Medium, 2: Hard
)
