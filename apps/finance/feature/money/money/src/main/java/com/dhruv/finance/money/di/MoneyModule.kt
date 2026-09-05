package com.dhruv.finance.money.di

import com.dhruv.finance.money.AccountDetailViewModel
import com.dhruv.finance.money.AccountFormViewModel
import com.dhruv.finance.money.AccountsViewModel
import com.dhruv.finance.money.CategoriesViewModel
import com.dhruv.finance.money.LedgerViewModel
import com.dhruv.finance.money.QuickAddViewModel
import com.dhruv.finance.money.ReceiptStore
import com.dhruv.finance.money.RecurringReviewViewModel
import com.dhruv.finance.money.RecurringViewModel
import com.dhruv.finance.money.TransactionDetailViewModel
import com.dhruv.finance.money.TransactionFormViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val moneyModule =
    module {
        viewModel { LedgerViewModel(get(), get(), get(), get()) }
        viewModel { QuickAddViewModel(get(), get(), get(), get(), get()) }
        viewModel { TransactionFormViewModel(get(), get(), get(), get(), get(), get()) }
        viewModel { TransactionDetailViewModel(get(), get(), get(), get(), get()) }
        viewModel { AccountsViewModel(get(), get(), get()) }
        viewModel { (accountId: String) -> AccountDetailViewModel(accountId, get(), get(), get(), get()) }
        viewModel { AccountFormViewModel(get(), get(), get()) }
        viewModel { CategoriesViewModel(get(), get(), get()) }
        viewModel { RecurringViewModel(get(), get(), get(), get()) }
        viewModel { RecurringReviewViewModel(get(), get(), get()) }
        single { ReceiptStore(androidContext()) }
    }
