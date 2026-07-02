package com.dhruv.settings

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module for the settings library.
 *
 * Usage in [Application.onCreate]:
 * ```kotlin
 * startKoin {
 *     androidContext(this@App)
 *     modules(appModule, settingsModule)
 * }
 * ```
 */
val settingsModule =
    module {
        single<SettingsRepository> {
            SettingsRepositoryImpl(
                context = androidContext(),
                crashReporter = get(),
            )
        }
    }
