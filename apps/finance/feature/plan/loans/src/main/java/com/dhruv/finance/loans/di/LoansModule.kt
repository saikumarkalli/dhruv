package com.dhruv.finance.loans.di

import com.dhruv.finance.loans.LoansViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val loansModule =
    module {
        viewModel { LoansViewModel(get(), get()) }
    }
