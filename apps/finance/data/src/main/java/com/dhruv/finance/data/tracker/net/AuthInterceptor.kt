package com.dhruv.finance.data.tracker.net

import com.dhruv.finance.data.tracker.auth.GoTrueApi
import com.dhruv.finance.data.tracker.auth.SessionStore
import com.dhruv.finance.data.tracker.dto.GoTrueSessionDto
import com.dhruv.finance.data.tracker.dto.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

private const val HTTP_UNAUTHORIZED = 401
private const val TOKEN_PATH_SUFFIX = "/token"

/**
 * Attaches Supabase auth headers to every tracker request and handles the single-refresh-then-
 * sign-out flow on 401 (DAT-BR-002, DAT-BR-003, ADR-0029).
 *
 * [refreshApi] is a provider, not a value, because of a construction-order cycle:
 * [com.dhruv.finance.data.tracker.net.SupabaseClientFactory] builds this interceptor, then an
 * OkHttpClient that carries it, then the [GoTrueApi] used *by this same interceptor* to perform
 * the refresh call — from that same client. The lambda is only invoked once construction has
 * fully completed (the first 401), so the forward reference is safe. Requests to the token
 * endpoint itself are never retried on 401 — that would recurse (a rejected refresh_token would
 * otherwise trigger another refresh attempt against itself, forever).
 */
class AuthInterceptor(
    private val sessionStore: SessionStore,
    private val anonKey: String,
    private val refreshApi: () -> GoTrueApi,
) : Interceptor {
    // Deliberately multiple early returns: the token-endpoint/non-401/no-refresh-token guard, the
    // failed-refresh sign-out, and the final retried response are three genuinely distinct exits,
    // not a single value threaded through nested conditionals. Same accepted pattern as
    // SessionStoreImpl's deriveTokens/deriveState (final whole-branch review, Fix 3).
    @Suppress("ReturnCount")
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val tokens = sessionStore.currentTokens()
        val response = chain.proceed(authorize(original, tokens?.accessToken))

        val isTokenEndpoint = original.url.encodedPath.endsWith(TOKEN_PATH_SUFFIX)
        if (isTokenEndpoint || response.code != HTTP_UNAUTHORIZED || tokens?.refreshToken == null) {
            return response
        }

        val refreshed = attemptRefresh(tokens.refreshToken)
        if (refreshed == null) {
            forceSignOut()
            return response
        }
        response.close()

        runBlocking { sessionStore.save(refreshed) }
        val retried = chain.proceed(authorize(original, refreshed.accessToken))
        if (retried.code == HTTP_UNAUTHORIZED) {
            forceSignOut()
        }
        return retried
    }

    private fun attemptRefresh(refreshToken: String): GoTrueSessionDto? =
        runCatching {
            runBlocking { refreshApi().refresh(body = RefreshTokenRequest(refreshToken)) }
        }.getOrNull()

    private fun forceSignOut() {
        runBlocking { sessionStore.clear() }
    }

    private fun authorize(
        request: Request,
        accessToken: String?,
    ): Request =
        request
            .newBuilder()
            .header("apikey", anonKey)
            .apply { if (accessToken != null) header("Authorization", "Bearer $accessToken") }
            .build()
}
