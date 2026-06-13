package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "ai_chat_messages",
    indices = [Index(value = ["timestamp"])]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val relatedCalculationId: Long? = null,
    val imageUri: String? = null // Reserved for future Phase 3 OCR capability
)
