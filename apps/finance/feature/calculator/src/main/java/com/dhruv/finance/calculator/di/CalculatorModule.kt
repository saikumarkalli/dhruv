package com.dhruv.finance.calculator.di

import com.dhruv.finance.calculator.CalculatorViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/** Koin module for the calculator feature. */
val calculatorModule = module {
    viewModel {
        CalculatorViewModel(
            historyRepository = get(),
            settingsRepository = get(),
            geminiRepository = get(),
            crashReporter = get(),
            performanceTracer = get()
        )
    }
}
