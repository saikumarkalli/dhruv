package com.dhruv.finance.date.di

import com.dhruv.finance.date.DateViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val dateModule =
    module {
        viewModel { DateViewModel(get(), get()) }
    }
