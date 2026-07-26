package com.dhruv.finance.app.di

import com.dhruv.finance.app.navigation.NavigationDispatcher
import com.dhruv.finance.app.ui.settings.SettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * App-shell Koin module. Only app-owned wiring lives here — Settings is part of the shell, not a
 * feature module. Platform singletons are in [platformModule]; the Room/repository layer is in
 * :apps:finance:data's dataModule; each feature provides its own module (see CalculatorApplication).
 */
val appModule =
    module {
        viewModel { SettingsViewModel(get(), get()) }
        single { NavigationDispatcher() }
    }
