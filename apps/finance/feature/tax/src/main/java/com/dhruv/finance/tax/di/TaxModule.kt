package com.dhruv.finance.tax.di

import com.dhruv.finance.tax.TaxViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/** Koin module for the tax feature. */
val taxModule = module {
    viewModel { TaxViewModel(get(), get()) }
}
