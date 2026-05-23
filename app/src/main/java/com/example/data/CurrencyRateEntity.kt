package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currency_rates")
data class CurrencyRateEntity(
    @PrimaryKey val currencyCode: String,
    val rate: Double,
    val timestamp: Long = System.currentTimeMillis()
)
