package com.dhruv.finance.unit.di

import com.dhruv.finance.unit.UnitViewModel
import com.dhruv.finance.unit.settings.unitSettingsContribution
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Koin module for the unit feature. */
val unitModule =
    module {
        viewModel { UnitViewModel(get(), get()) }
        // Qualified by moduleKey — see CalculatorModule.kt's comment (research R1).
        single(qualifier = named("unit")) { unitSettingsContribution() }
    }
