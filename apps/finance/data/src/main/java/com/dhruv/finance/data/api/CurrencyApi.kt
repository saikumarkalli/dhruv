package com.dhruv.finance.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ExchangeRateResponse(
    @param:Json(name = "result") val result: String,
    @param:Json(name = "base_code") val baseCode: String,
    @param:Json(name = "rates") val rates: Map<String, Double>
)

@JsonClass(generateAdapter = true)
data class ExchangeRateResponseFallback(
    @param:Json(name = "base") val base: String,
    @param:Json(name = "rates") val rates: Map<String, Double>
)

interface CurrencyApi {
    @GET("v6/latest/{base}")
    suspend fun getLatestRates(@Path("base") base: String): ExchangeRateResponse
}

interface CurrencyApiFallback {
    @GET("v4/latest/{base}")
    suspend fun getLatestRatesFallback(@Path("base") base: String): ExchangeRateResponseFallback
}

class CurrencyApiClient(
    primaryBaseUrl: String,
    fallbackBaseUrl: String,
    timeoutSeconds: Long,
    userAgent: String,
) {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    val api: CurrencyApi = retrofit(primaryBaseUrl).create(CurrencyApi::class.java)
    val fallbackApi: CurrencyApiFallback = retrofit(fallbackBaseUrl).create(CurrencyApiFallback::class.java)
}
