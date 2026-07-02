package com.dhruv.finance.investments.di

import com.dhruv.finance.investments.InvestmentsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val investmentsModule =
    module {
        viewModel { InvestmentsViewModel(get(), get()) }
    }
