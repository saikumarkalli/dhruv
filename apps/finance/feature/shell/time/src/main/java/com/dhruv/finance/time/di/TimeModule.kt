package com.dhruv.finance.time.di

import com.dhruv.finance.time.TimeViewModel
import com.dhruv.finance.time.stopwatch.StopwatchViewModel
import com.dhruv.finance.time.timer.TimerViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val timeModule =
    module {
        viewModel { TimeViewModel(get()) }
        viewModel { StopwatchViewModel(get(), get()) }
        viewModel { TimerViewModel(get(), get()) }
    }
