package com.dhruv.finance.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GoogleGenerativeAIException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Wraps the online Gemini model for short calculation/finance explanations.
 *
 * The API key is injected (not read from BuildConfig) so this repository can live in
 * :apps:finance:data and be shared by the calculator (in-screen AI explain) and the
 * standalone assistant feature without either feature depending on the app module. The
 * app supplies BuildConfig.GEMINI_API_KEY when it registers this in Koin.
 */
class GeminiRepository(
    private val apiKey: String
) {

    private companion object {
        /**
         * Moving alias that always points at the current Gemini Flash model, so a future
         * model retirement never 404s us again. (The pinned "gemini-1.5-flash" was retired
         * from the v1beta surface this SDK targets, which produced a NOT_FOUND/404.)
         */
        const val MODEL_NAME = "gemini-flash-latest"
    }

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = MODEL_NAME,
            apiKey = apiKey
        )
    }

    suspend fun explainCalculation(expression: String, result: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
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

            val response = generativeModel.generateContent(prompt)
            val text = response.text
            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(Exception("Gemini returned an empty response."))
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
