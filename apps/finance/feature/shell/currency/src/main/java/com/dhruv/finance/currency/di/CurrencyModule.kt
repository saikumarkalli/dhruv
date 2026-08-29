package com.dhruv.finance.currency.di

import com.dhruv.finance.currency.CurrencyViewModel
import com.dhruv.finance.currency.settings.currencySettingsContribution
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Koin module for the currency feature.
 * Constructor order: ICurrencyRepository, CrashReporter, PerformanceTracer
 */
val currencyModule =
    module {
        viewModel { CurrencyViewModel(get(), get(), get()) }
        // Qualified by moduleKey — see CalculatorModule.kt's comment (research R1).
        single(qualifier = named("currency")) { currencySettingsContribution(settingsRepository = get()) }
    }
