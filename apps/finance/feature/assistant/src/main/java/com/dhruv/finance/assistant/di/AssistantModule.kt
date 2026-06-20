package com.dhruv.finance.assistant.di

import com.dhruv.finance.assistant.AssistantViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val assistantModule = module {
    viewModel { AssistantViewModel(get(), get(), get()) }
}
