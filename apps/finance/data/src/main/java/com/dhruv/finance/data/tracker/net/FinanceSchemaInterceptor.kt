package com.dhruv.finance.data.tracker.net

import okhttp3.Interceptor
import okhttp3.Response

private const val FINANCE_SCHEMA = "finance"
private val MUTATION_METHODS = setOf("POST", "PATCH", "PUT", "DELETE")

/**
 * Adds PostgREST's schema-select headers for the `finance` Postgres schema (ADR-0033) —
 * `Accept-Profile` on every request, `Content-Profile` additionally on mutations. Without these, a
 * `finance.*` table/view/RPC call silently 404s against the (empty) `public` schema instead of
 * erroring loudly — [SupabaseClientFactory.dataRetrofit]'s own doc comment flagged this as the one
 * thing every Phase 2+ endpoint on that client must not forget. An interceptor makes it structural
 * instead of per-endpoint discipline, same reasoning as [ConsentInterceptor]/[AuthInterceptor].
 *
 * Attached only to [SupabaseClientFactory.dataClient] — [SupabaseClientFactory.erasureRetrofit]'s
 * two RPCs deliberately stay in `public` (ADR-0033) and must never get this header.
 */
class FinanceSchemaInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val request =
            original
                .newBuilder()
                .header("Accept-Profile", FINANCE_SCHEMA)
                .apply { if (original.method in MUTATION_METHODS) header("Content-Profile", FINANCE_SCHEMA) }
                .build()
        return chain.proceed(request)
    }
}
