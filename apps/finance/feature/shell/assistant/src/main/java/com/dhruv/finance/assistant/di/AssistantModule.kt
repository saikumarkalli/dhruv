package com.dhruv.finance.assistant.di

import com.dhruv.finance.assistant.AssistantViewModel
import com.dhruv.finance.assistant.settings.assistantSettingsContribution
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val assistantModule =
    module {
        viewModel { AssistantViewModel(get(), get(), get(), get()) }
        // Qualified by moduleKey — see CalculatorModule.kt's comment (research R1).
        single(qualifier = named("assistant")) { assistantSettingsContribution(settingsRepository = get()) }
    }
