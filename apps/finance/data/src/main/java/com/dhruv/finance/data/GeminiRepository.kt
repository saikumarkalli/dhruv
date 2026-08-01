package com.dhruv.finance.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GoogleGenerativeAIException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

// Broad catches are intentional: this is a network/AI boundary where any failure (timeout,
// quota, parse, SDK error) must degrade to a user-facing Result, never crash the feature.

/**
 * Wraps the online Gemini model for short calculation/finance explanations.
 *
 * The API key is injected (not read from BuildConfig) so this repository can live in
 * :apps:finance:data and be shared by the calculator (in-screen AI explain) and the
 * standalone assistant feature without either feature depending on the app module. The
 * app supplies BuildConfig.GEMINI_API_KEY when it registers this in Koin.
 */
@Suppress("TooGenericExceptionCaught")
class GeminiRepository(
    private val apiKey: String,
) {
    private companion object {
        /**
         * Moving alias that always points at the current Gemini Flash model, so a future
         * model retirement never 404s us again. (The pinned "gemini-1.5-flash" was retired
         * from the v1beta surface this SDK targets, which produced a NOT_FOUND/404.)
         */
        const val MODEL_NAME = "gemini-flash-latest"
        const val UNCONFIGURED_API_KEY_SENTINEL = "MY_GEMINI_API_KEY"
    }

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = MODEL_NAME,
            apiKey = apiKey,
        )
    }

    suspend fun explainCalculation(
        expression: String,
        result: String,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                if (apiKey.isBlank() || apiKey == UNCONFIGURED_API_KEY_SENTINEL) {
                    return@withContext Result.failure(Exception("Gemini API key is not configured. Please add it to your .env file."))
                }

                val prompt =
                    """
                    You are a premium, friendly mathematical and financial assistant.
                    Explain the following calculation clearly and concisely for an Android app screen:
                    Expression: $expression
                    Result: $result
                    Provide a short 2-3 sentence breakdown of what this calculation means.
                    If it's standard arithmetic, briefly explain the steps. If it's trigonometry or logs, explain the function's meaning.
                    If it looks like a financial calculation (e.g. interest or compounding), explain the financial implication.
                    Keep the response concise, engaging, and format it nicely. Do not use markdown titles.
                    """.trimIndent()

                generateText(prompt)
            } catch (e: Exception) {
                mapError(e)
            }
        }

    /**
     * Solves whatever the user typed into the calculator — including natural-language math/finance
     * queries the offline engine can't parse (e.g. "15% tip on 1240 split by 3"). Returns a clean,
     * minimal answer (the final value on its own line) followed by one short sentence of context,
     * rather than a step-by-step explanation.
     */
    suspend fun solve(input: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                if (apiKey.isBlank() || apiKey == UNCONFIGURED_API_KEY_SENTINEL) {
                    return@withContext Result.failure(Exception("Gemini API key is not configured. Please add it to your .env file."))
                }
                if (input.isBlank()) {
                    return@withContext Result.failure(Exception("Type something to solve first."))
                }

                val prompt =
                    """
                    You are a precise calculator and finance assistant inside an Android app.
                    The user typed this into the calculator (it may be a plain math expression or a
                    natural-language question):

                    "$input"

                    Compute or resolve it and reply with a clean, neat answer. Reply with EXACTLY:
                    - First line: the final answer only — the number or value, with a currency symbol or
                      unit if one is implied. Nothing else on this line.
                    - A blank line, then one short plain-language sentence (max ~20 words) of context.

                    Do not show your working or steps. Do not use markdown, headings, bullets, or labels
                    like "Answer:". Do not restate the question. If the input can't be solved or is
                    ambiguous, say so in one short line instead.
                    """.trimIndent()

                generateText(prompt)
            } catch (e: Exception) {
                mapError(e)
            }
        }

    private suspend fun generateText(prompt: String): Result<String> {
        val response = generativeModel.generateContent(prompt)
        val text = response.text
        return if (!text.isNullOrBlank()) {
            Result.success(text.trim())
        } else {
            Result.failure(Exception("Gemini returned an empty response."))
        }
    }

    private fun mapError(e: Exception): Result<String> =
        when (e) {
            is IOException ->
                Result.failure(Exception("Network error. Please check your internet connection and try again."))
            is GoogleGenerativeAIException ->
                if (e.message?.contains("quota", ignoreCase = true) == true) {
                    Result.failure(Exception("API quota exceeded. Please try again later."))
                } else {
                    Result.failure(Exception("Gemini API error: ${e.localizedMessage}"))
                }
            else -> Result.failure(e)
        }
}
