package com.dhruv.finance.app.ui.settings

/**
 * Screen-level data for Settings (Article V — never inline in a screen file).
 */
object SettingsConfig {
    /** FR-002: exactly these three, in this order — never grown or reordered outside a spec change. */
    enum class QuickRow { THEME, ACCENT, APP_LOCK }

    val quickRows = listOf(QuickRow.THEME, QuickRow.ACCENT, QuickRow.APP_LOCK)

    /** FR-001 top-level order, after the quick rows. */
    enum class Tier { ACCOUNT, APP, MODULES }

    val tierOrder = listOf(Tier.ACCOUNT, Tier.APP, Tier.MODULES)

    /** Auto-lock timeout option ids (data-model.md §3) — append-only, never rename a shipped id. */
    object LockTimeoutOptions {
        const val IMMEDIATE = "immediate"
        const val AFTER_1_MIN = "after_1_min"
        const val AFTER_5_MIN = "after_5_min"
        const val AFTER_15_MIN = "after_15_min"

        val ids = listOf(IMMEDIATE, AFTER_1_MIN, AFTER_5_MIN, AFTER_15_MIN)
    }
}
