package com.dhruv.finance.everyday.di

import com.dhruv.finance.everyday.EverydayViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/** Koin module for the everyday feature. */
val everydayModule = module {
    viewModel { EverydayViewModel(get(), get()) }
}
