package com.example.ui.aichat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AiChatViewModel(
    private val repository: AiChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.chatHistory.collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage(contextString: String? = null) {
        val query = _uiState.value.inputText
        if (query.isBlank()) return

        // Clear input immediately
        _uiState.update { it.copy(inputText = "", isTyping = true) }

        viewModelScope.launch {
            repository.sendMessage(query, contextString)
            _uiState.update { it.copy(isTyping = false) }
        }
    }

    fun sendSuggestedMessage(query: String, contextString: String? = null) {
        _uiState.update { it.copy(isTyping = true) }
        viewModelScope.launch {
            repository.sendMessage(query, contextString)
            _uiState.update { it.copy(isTyping = false) }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatHistory()
        }
    }
}
