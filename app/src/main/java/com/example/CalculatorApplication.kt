package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.CurrencyRepository
import com.example.data.HistoryRepository
import com.example.data.SettingsRepository

class CalculatorApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val historyRepository by lazy { HistoryRepository(database.historyDao()) }
    val currencyRepository by lazy { CurrencyRepository(database.currencyRateDao()) }
    val settingsRepository by lazy { SettingsRepository(this) }
}
