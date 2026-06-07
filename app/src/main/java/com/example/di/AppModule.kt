package com.example.di

import com.example.data.*
import com.example.ui.calculator.CalculatorViewModel
import com.example.ui.converter.ConverterViewModel
import com.example.ui.date.DateViewModel
import com.example.ui.finance.FinanceViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Database and DAOs
    single { AppDatabase.getDatabase(androidContext()) }
    single { get<AppDatabase>().historyDao() }
    single { get<AppDatabase>().currencyRateDao() }

    // Repositories
    single { HistoryRepository(get()) }
    single<ICurrencyRepository> { CurrencyRepository(get()) }
    single { SettingsRepository(androidContext()) }
    single { GeminiRepository() }

    // ViewModels
    viewModel { CalculatorViewModel(get(), get(), get()) }
    viewModel { ConverterViewModel(get()) }
    viewModel { DateViewModel() }
    viewModel { FinanceViewModel() }
}
