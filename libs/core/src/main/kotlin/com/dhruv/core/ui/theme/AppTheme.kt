package com.dhruv.core.ui.theme

/** Global theme preference. Maps to DataStore key "dark_mode". */
enum class AppTheme {
    /** Force dark theme regardless of system setting. DataStore value: "always_dark". */
    DARK,

    /** Force light theme regardless of system setting. DataStore value: "always_light". */
    LIGHT,

    /** Follow the system dark/light setting (default). DataStore value: "system". */
    SYSTEM
}
