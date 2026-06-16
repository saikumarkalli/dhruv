package com.dhruv.finance.currency.di

import com.dhruv.finance.currency.CurrencyViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for the currency feature.
 * Constructor order: ICurrencyRepository, CrashReporter, PerformanceTracer
 */
val currencyModule = module {
    viewModel { CurrencyViewModel(get(), get(), get()) }
}
