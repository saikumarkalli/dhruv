package com.dhruv.settings

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `SET-ARCH-006`: today's `SettingsKeys` key set, frozen before Phase 0b's migration, must remain a
 * subset of the shipped key set. This is the only guard against a row silently resetting every
 * user's preference when it moves to its new tier (constitution IX, data-model.md §1).
 *
 * Nine keys were intentionally **removed**, not migrated, in 0b.5 (T110's orphan-preference audit —
 * `color_calculator`/`color_converter`/`color_date`/`color_finance`/`color_time`, retired by
 * ADR-0024's single global accent, and `is_converter_enabled`/`is_date_enabled`/
 * `is_finance_enabled`/`is_time_enabled`, retired with the old 5-tab pager). Verified zero
 * consumers anywhere in the app by full-repo grep before removal — this is a deliberate exception
 * to "never drop a key," not an oversight, so both lists below reflect their absence rather than
 * the subset check failing on them.
 */
class SettingsKeyPreservationTest {
    private val preChangeKeyNames =
        setOf(
            "is_degree",
            "dark_mode",
            "decimal_precision",
            "is_history_locked",
            "history_pin_code",
            "format_locale",
            "font_family",
            "biometric_enabled",
            "sync_enabled",
            "accent_color_hex",
        )

    @Test
    fun `today's key set is a subset of the shipped key set`() {
        val shippedKeyNames =
            setOf(
                SettingsKeys.IS_DEGREE.name,
                SettingsKeys.DARK_MODE.name,
                SettingsKeys.DECIMAL_PRECISION.name,
                SettingsKeys.IS_HISTORY_LOCKED.name,
                SettingsKeys.HISTORY_PIN_CODE.name,
                SettingsKeys.FORMAT_LOCALE.name,
                SettingsKeys.FONT_FAMILY.name,
                SettingsKeys.BIOMETRIC_ENABLED.name,
                SettingsKeys.SYNC_ENABLED.name,
                SettingsKeys.ACCENT_COLOR_HEX.name,
                SettingsKeys.APP_LOCK_TIMEOUT.name,
                SettingsKeys.HIDE_AMOUNTS.name,
                SettingsKeys.NOTIFICATIONS_MASTER.name,
                SettingsKeys.ASSISTANT_CONSENT_GRANTED.name,
            )

        assertTrue(
            "shipped key set must contain every pre-change key: missing " +
                (preChangeKeyNames - shippedKeyNames),
            shippedKeyNames.containsAll(preChangeKeyNames),
        )
    }
}
