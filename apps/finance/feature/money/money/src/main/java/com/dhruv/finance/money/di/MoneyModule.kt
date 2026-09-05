package com.dhruv.finance.money.di

import com.dhruv.finance.money.LedgerViewModel
import com.dhruv.finance.money.QuickAddViewModel
import com.dhruv.finance.money.TransactionFormViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val moneyModule =
    module {
        viewModel { LedgerViewModel(get(), get(), get(), get()) }
        viewModel { QuickAddViewModel(get(), get(), get(), get(), get()) }
        viewModel { TransactionFormViewModel(get(), get(), get(), get(), get()) }
    }
