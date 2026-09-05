package com.dhruv.finance.data.tracker.net

import okhttp3.Interceptor
import okhttp3.Response

private val WRITE_METHODS = setOf("POST", "PATCH", "PUT", "DELETE")

/**
 * Attaches PostgREST's schema-select header on every request to `SupabaseClientFactory.dataClient`
 * (ADR-0033): `Accept-Profile: finance` on reads, `Content-Profile: finance` on writes. Every
 * `finance.*` table/view/function lives outside the default `public` schema — omitting this header
 * does not error, it silently 404s against the (empty) `public` schema instead, which is exactly
 * the failure mode this interceptor exists to make structurally impossible rather than a thing
 * every new repository has to remember (same reasoning as [ConsentInterceptor]).
 *
 * `rpc/merge_categories`, `rpc/delete_my_data` etc. also live under `finance`/`public` respectively
 * — `delete_my_data`/`delete_my_account` are called via [SupabaseClientFactory.erasureRetrofit],
 * which does NOT carry this interceptor (they are `public`-schema by design, ADR-0033), so this
 * interceptor is attached only to `dataClient`, never `authClient`.
 */
class FinanceSchemaInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val headerName = if (original.method in WRITE_METHODS) "Content-Profile" else "Accept-Profile"
        val request = original.newBuilder().header(headerName, "finance").build()
        return chain.proceed(request)
    }
}
