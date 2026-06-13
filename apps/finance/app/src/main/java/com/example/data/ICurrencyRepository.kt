package com.example.data

interface ICurrencyRepository {
    /**
     * Retrieves all currency rate entities stored in the local cache.
     */
    suspend fun getAllRates(): List<CurrencyRateEntity>

    /**
     * Gets the rate of a specific currency relative to the base currency.
     */
    suspend fun getRate(code: String): Double?

    /**
     * Performs a network fetch to synchronize local rates.
     * Uses a self-healing fallback mechanism.
     */
    suspend fun fetchAndCacheLatestRates(baseCurrency: String = "USD"): Result<Map<String, Double>>
}
