package com.dhruv.finance.unit.di

import com.dhruv.finance.unit.UnitViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/** Koin module for the unit feature. */
val unitModule =
    module {
        viewModel { UnitViewModel(get(), get()) }
    }
