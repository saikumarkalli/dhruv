package com.dhruv.finance.data.di

import com.dhruv.finance.data.AppDatabase
import com.dhruv.finance.data.CurrencyRepository
import com.dhruv.finance.data.ICurrencyRepository
import org.koin.dsl.module

// AppDatabase, CurrencyApiClient, and HistoryRepository are bound in the app module's
// platformModule instead of here: they need BuildConfig-sourced values, which this module
// (:apps:finance:data) cannot read. Koin merges all modules into one container, so DAOs/
// repositories below can still resolve them via get(). See PlatformModule.kt.
val dataModule = module {
    // DAOs
    single { get<AppDatabase>().historyDao() }
    single { get<AppDatabase>().currencyRateDao() }

    // Repositories
    single<ICurrencyRepository> { CurrencyRepository(get(), get()) }
}
