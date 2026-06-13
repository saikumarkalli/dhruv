package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content

class AiChatRepository(
    private val chatDao: ChatDao,
    private val geminiRepository: GeminiRepository
) {
    val chatHistory: Flow<List<ChatMessageEntity>> = chatDao.getAllMessages()

    private val modelNames = listOf("gemini-1.5-flash", "gemini-1.5-pro", "gemini-pro")

    private fun getModel(modelName: String): GenerativeModel {
        return GenerativeModel(
            modelName = modelName,
            apiKey = BuildConfig.GEMINI_API_KEY,
            systemInstruction = content { text("You are a highly specialized, concise financial and mathematical advisor for an Android calculator app. Provide actionable insights. Use markdown to format formulas and bold important numbers. Keep responses short and directly answer the question.") }
        )
    }

    suspend fun sendMessage(query: String, contextString: String? = null) {
        withContext(Dispatchers.IO) {
            // 1. Save user message
            val userMsg = ChatMessageEntity(text = query, isFromUser = true)
            chatDao.insertMessage(userMsg)

            // 2. Fetch AI response
            try {
                if (BuildConfig.GEMINI_API_KEY.isBlank() || BuildConfig.GEMINI_API_KEY == "MY_GEMINI_API_KEY") {
                    chatDao.insertMessage(ChatMessageEntity(text = "Error: Gemini API key is not configured in .env file.", isFromUser = false))
                    return@withContext
                }
                
                val prompt = if (contextString != null) {
                    "Context (Recent calculation): $contextString\n\nUser Question: $query"
                } else {
                    query
                }

                var responseText: String? = null
                var lastException: Exception? = null

                for (modelName in modelNames) {
                    try {
                        val model = getModel(modelName)
                        val response = model.generateContent(prompt)
                        responseText = response.text
                        if (!responseText.isNullOrBlank()) {
                            break // Success, exit loop
                        }
                    } catch (e: Exception) {
                        lastException = e
                        if (e.message?.contains("quota", ignoreCase = true) == true) {
                            break // Don't fallback on quota errors
                        }
                    }
                }

                if (responseText.isNullOrBlank()) {
                    throw lastException ?: Exception("All Gemini models returned an empty response.")
                }
                
                // 3. Save AI response
                chatDao.insertMessage(ChatMessageEntity(text = responseText, isFromUser = false))
            } catch (e: Exception) {
                val errorMsg = if (e.message?.contains("quota", ignoreCase = true) == true) {
                    "Error: API quota exceeded. Please try again later."
                } else if (e is java.io.IOException) {
                    "Network error. Please check your internet connection."
                } else {
                    "Error: ${e.localizedMessage}"
                }
                chatDao.insertMessage(ChatMessageEntity(text = errorMsg, isFromUser = false))
            }
        }
    }

    suspend fun clearChatHistory() {
        withContext(Dispatchers.IO) {
            chatDao.deleteAllMessages()
        }
    }
}
