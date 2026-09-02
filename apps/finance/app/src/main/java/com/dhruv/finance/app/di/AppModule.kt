package com.dhruv.finance.app.di

import android.os.Build
import com.dhruv.core.navigation.NavigationDispatcher
import com.dhruv.finance.app.ui.settings.AccountSettingsViewModel
import com.dhruv.finance.app.ui.settings.AppDetailsViewModel
import com.dhruv.finance.app.ui.settings.AppSettingsViewModel
import com.dhruv.finance.app.ui.settings.SettingsContributionSource
import com.dhruv.finance.app.ui.settings.SettingsViewModel
import com.dhruv.finance.app.ui.settings.hasEnrolledCredential
import com.dhruv.settings.contribution.SettingsRegistry
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * App-shell Koin module. Only app-owned wiring lives here — Settings is part of the shell, not a
 * feature module. Platform singletons are in [platformModule]; the Room/repository layer is in
 * :apps:finance:data's dataModule; each feature provides its own module (see CalculatorApplication).
 *
 * [SettingsContributionSource] is the one place a module's `SettingsContribution` is resolved by
 * type from the DI container (research R1) rather than from a hardcoded list (FR-004) — it wraps
 * the platform `Koin` instance so [com.dhruv.finance.app.ui.settings.SettingsScreen] never touches
 * Koin's container API directly.
 */
val appModule =
    module {
        viewModel { SettingsViewModel(get(), get()) }
        viewModel {
            AppSettingsViewModel(
                settingsRepository = get(),
                hasEnrolledCredential = { hasEnrolledCredential(androidContext()) },
                crashReporter = get(),
            )
        }
        viewModel { AccountSettingsViewModel(get(), get(), get(), get(), get()) }
        viewModel {
            // PackageInfo read here, not in the ViewModel, so the ViewModel stays Context-free and
            // JVM-testable (AppDetailsViewModelTest). `updateChecker = null` is the real shipped
            // wiring — no update channel exists yet (ADR-0008), so App details omits the row
            // entirely rather than showing an inert one (FR-043).
            val context = androidContext()
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            AppDetailsViewModel(
                versionName = packageInfo.versionName ?: "1.0",
                versionCode =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode.toLong()
                    },
                updateChecker = null,
            )
        }
        single { NavigationDispatcher() }
        single { SettingsContributionSource(getKoin()) }
        single { SettingsRegistry() }
    }
