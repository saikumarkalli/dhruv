package com.dhruv.finance.app

import android.app.Application
import com.dhruv.finance.app.di.appModule
import com.dhruv.finance.app.di.platformModule
import com.dhruv.finance.assistant.di.assistantModule
import com.dhruv.finance.calculator.di.calculatorModule
import com.dhruv.finance.currency.di.currencyModule
import com.dhruv.finance.data.di.dataModule
import com.dhruv.finance.date.di.dateModule
import com.dhruv.finance.everyday.di.everydayModule
import com.dhruv.finance.investments.di.investmentsModule
import com.dhruv.finance.loans.di.loansModule
import com.dhruv.finance.networth.di.netWorthModule
import com.dhruv.finance.onboarding.di.onboardingModule
import com.dhruv.finance.tax.di.taxModule
import com.dhruv.finance.time.di.timeModule
import com.dhruv.finance.unit.di.unitModule
import com.dhruv.settings.settingsModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class CalculatorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@CalculatorApplication)
            modules(
                // Platform + shared
                platformModule,
                dataModule,
                settingsModule,
                appModule,
                // Feature modules
                calculatorModule,
                loansModule,
                investmentsModule,
                taxModule,
                everydayModule,
                currencyModule,
                unitModule,
                dateModule,
                timeModule,
                assistantModule,
                onboardingModule,
                netWorthModule,
            )
        }
    }
}
