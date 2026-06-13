package com.example.ui.aichat

import com.example.data.ChatMessageEntity

data class AiChatUiState(
    val messages: List<ChatMessageEntity> = emptyList(),
    val isTyping: Boolean = false,
    val contextChips: List<String> = listOf("Explain EMI", "Tax savings tips", "Compound interest formula"),
    val inputText: String = ""
)
