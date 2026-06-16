package com.dhruv.finance.time.di

import com.dhruv.finance.time.TimeViewModel
import com.dhruv.finance.time.alarm.AlarmViewModel
import com.dhruv.finance.time.service.alarm.AlarmScheduler
import com.dhruv.finance.time.service.alarm.AlarmSchedulerImpl
import com.dhruv.finance.time.stopwatch.StopwatchViewModel
import com.dhruv.finance.time.timer.TimerViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val timeModule = module {
    single<AlarmScheduler> { AlarmSchedulerImpl(androidContext()) }

    viewModel { TimeViewModel(get(), get()) }
    viewModel { StopwatchViewModel(get(), get()) }
    viewModel { TimerViewModel(get(), get()) }
    viewModel { AlarmViewModel(get(), get(), get(), get()) }
}
