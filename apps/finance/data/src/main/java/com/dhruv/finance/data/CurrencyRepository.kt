package com.dhruv.finance.data

import android.util.Log
import com.dhruv.finance.data.api.CurrencyApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CurrencyRepository(private val currencyRateDao: CurrencyRateDao) : ICurrencyRepository {

    override suspend fun getAllRates(): List<CurrencyRateEntity> = withContext(Dispatchers.IO) {
        return@withContext currencyRateDao.getAllRates()
    }

    override suspend fun getRate(code: String): Double? = withContext(Dispatchers.IO) {
        return@withContext currencyRateDao.getRateByCode(code)?.rate
    }

    /**
     * Tries to fetch latest rates, caches them locally.
     * Uses double-api self-healing fallback layers.
     * If offline, fallback to locally stored rates.
     */
    override suspend fun fetchAndCacheLatestRates(baseCurrency: String): Result<Map<String, Double>> = withContext(Dispatchers.IO) {
        try {
            val response = CurrencyApiClient.api.getLatestRates(baseCurrency)
            if (response.result == "success") {
                val entities = response.rates.map { (code, rate) ->
                    CurrencyRateEntity(currencyCode = code, rate = rate, timestamp = System.currentTimeMillis())
                }
                currencyRateDao.clearAllRates()
                currencyRateDao.insertRates(entities)
                return@withContext Result.success(response.rates)
            } else {
                throw Exception("Primary API returned unsuccessful code")
            }
        } catch (primaryException: Exception) {
            Log.e("CurrencyRepository", "Primary API failed, trying fallback API...", primaryException)
            try {
                val responseFallback = CurrencyApiClient.api.getLatestRatesFallback(baseCurrency)
                val entities = responseFallback.rates.map { (code, rate) ->
                    CurrencyRateEntity(currencyCode = code, rate = rate, timestamp = System.currentTimeMillis())
                }
                currencyRateDao.clearAllRates()
                currencyRateDao.insertRates(entities)
                return@withContext Result.success(responseFallback.rates)
            } catch (fallbackException: Exception) {
                Log.e("CurrencyRepository", "Fallback API failed too, utilizing database cache representation", fallbackException)
                val cached = currencyRateDao.getAllRates()
                if (cached.isNotEmpty()) {
                    val map = cached.associate { it.currencyCode to it.rate }
                    return@withContext Result.success(map)
                }
                return@withContext Result.failure(fallbackException)
            }
        }
    }
}
