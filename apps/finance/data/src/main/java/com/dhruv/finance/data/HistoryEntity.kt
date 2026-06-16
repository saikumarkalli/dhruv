package com.dhruv.finance.data

import androidx.room.Entity
import androidx.room.PrimaryKey

import androidx.room.Index

@Entity(
    tableName = "calculation_history",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["favorite"]),
        Index(value = ["isInRecycleBin"])
    ]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isScientific: Boolean = false,
    val calculationType: String = if (isScientific) "scientific" else "standard",
    val favorite: Boolean = false,
    val edited: Boolean = false,
    val tags: String = "",
    val deviceSource: String = "Android Device",
    val note: String = "",
    val isInRecycleBin: Boolean = false,
    val deletedTimestamp: Long = 0L
)
