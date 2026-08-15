package com.dhruv.finance.data.tracker.auth

import retrofit2.http.POST

/**
 * PostgREST RPC endpoints for the two security-definer erasure functions defined in
 * `supabase/migrations/0001_init.sql` (ADR-0029 decision 5): `delete_my_data()` and
 * `delete_my_account()`. Both Postgres functions `returns void` and take no arguments, so neither
 * suspend method here has a request or response body — PostgREST's RPC convention is a bare
 * `POST rpc/<function_name>` with an empty JSON body. Built off [SupabaseClientFactory]'s
 * `erasureRetrofit` (auth-gated but deliberately NOT consent-gated) — erasure must stay reachable
 * even when "Sync my financial records" is off, unlike every other tracker data call, which goes
 * through the consent-gated `dataRetrofit` instead.
 */
interface TrackerRpcApi {
    @POST("rpc/delete_my_data")
    suspend fun deleteMyData()

    @POST("rpc/delete_my_account")
    suspend fun deleteMyAccount()
}
