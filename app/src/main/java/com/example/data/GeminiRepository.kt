package com.example.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.GoogleGenerativeAIException
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class GeminiRepository {

    private val modelNames = listOf("gemini-1.5-flash", "gemini-1.5-pro", "gemini-pro")

    private fun getModel(modelName: String): GenerativeModel {
        return GenerativeModel(
            modelName = modelName,
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    suspend fun explainCalculation(expression: String, result: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (BuildConfig.GEMINI_API_KEY.isBlank() || BuildConfig.GEMINI_API_KEY == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(Exception("Gemini API key is not configured. Please add it to your .env file."))
            }

            val prompt = """
                You are a premium, friendly mathematical and financial assistant.
                Explain the following calculation clearly and concisely for an Android app screen:
                Expression: $expression
                Result: $result
                Provide a short 2-3 sentence breakdown of what this calculation means. 
                If it's standard arithmetic, briefly explain the steps. If it's trigonometry or logs, explain the function's meaning. 
                If it looks like a financial calculation (e.g. interest or compounding), explain the financial implication.
                Keep the response concise, engaging, and format it nicely. Do not use markdown titles.
            """.trimIndent()

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

            if (!responseText.isNullOrBlank()) {
                Result.success(responseText)
            } else {
                throw lastException ?: Exception("All Gemini models returned an empty response.")
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error. Please check your internet connection and try again."))
        } catch (e: GoogleGenerativeAIException) {
            if (e.message?.contains("quota", ignoreCase = true) == true) {
                Result.failure(Exception("API quota exceeded. Please try again later."))
            } else {
                Result.failure(Exception("Gemini API error: ${e.localizedMessage}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
