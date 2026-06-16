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

    @GET("https://api.exchangerate-api.com/v4/latest/{base}")
    suspend fun getLatestRatesFallback(@Path("base") base: String): ExchangeRateResponseFallback
}

object CurrencyApiClient {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    val api: CurrencyApi = Retrofit.Builder()
        .baseUrl("https://open.er-api.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(CurrencyApi::class.java)
}
