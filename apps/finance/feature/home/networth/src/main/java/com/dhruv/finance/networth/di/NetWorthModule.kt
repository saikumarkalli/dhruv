package com.dhruv.finance.networth.di

import com.dhruv.finance.networth.AddEditHoldingViewModel
import com.dhruv.finance.networth.AssetsViewModel
import com.dhruv.finance.networth.NetWorthOverviewViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val netWorthModule =
    module {
        viewModel { NetWorthOverviewViewModel(get(), get(), get(), get(), get()) }
        viewModel { AssetsViewModel(get(), get(), get()) }
        viewModel { AddEditHoldingViewModel(get(), get(), get()) }
    }
