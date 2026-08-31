package com.dhruv.finance.calculator.di

import com.dhruv.finance.calculator.CalculatorViewModel
import com.dhruv.finance.calculator.settings.calculatorSettingsContribution
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Koin module for the calculator feature. */
val calculatorModule =
    module {
        viewModel {
            CalculatorViewModel(
                historyRepository = get(),
                settingsRepository = get(),
                geminiRepository = get(),
                crashReporter = get(),
                performanceTracer = get(),
            )
        }
        // Qualified by moduleKey (research R1, verified 2026-08-27) — an unqualified `single` here
        // would collide with every other module's SettingsContribution registration.
        single(qualifier = named("calculator")) {
            calculatorSettingsContribution(settingsRepository = get(), historyRepository = get())
        }
    }
