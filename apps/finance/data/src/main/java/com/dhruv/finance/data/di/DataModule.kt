package com.dhruv.finance.data.di

import com.dhruv.finance.data.AppDatabase
import com.dhruv.finance.data.CurrencyRepository
import com.dhruv.finance.data.HistoryRepository
import com.dhruv.finance.data.ICurrencyRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    // Database
    single { AppDatabase.getDatabase(androidContext()) }

    // DAOs
    single { get<AppDatabase>().historyDao() }
    single { get<AppDatabase>().currencyRateDao() }
    single { get<AppDatabase>().alarmDao() }

    // Repositories
    single { HistoryRepository(get()) }
    single<ICurrencyRepository> { CurrencyRepository(get()) }
}
