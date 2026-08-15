package com.dhruv.finance.onboarding.di

import com.dhruv.finance.onboarding.OnboardingViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val onboardingModule =
    module {
        viewModel { OnboardingViewModel(get(), get(), get(), get()) }
    }
