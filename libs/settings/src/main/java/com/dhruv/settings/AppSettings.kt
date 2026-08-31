package com.dhruv.settings

import com.dhruv.core.ui.theme.AppTheme
import com.dhruv.core.ui.theme.DhruvFont

/**
 * Immutable snapshot of all user-configurable settings.
 *
 * Mapping from DataStore keys:
 * - [theme]: "dark_mode" key — "always_dark"↔DARK, "always_light"↔LIGHT, "system"↔SYSTEM
 * - [accentColorHex]: "accent_color_hex" key — DhruvNext orange "#F05A28" by default (ADR-0024;
 *   numerically identical to [com.dhruv.core.ui.theme.PrimaryLight])
 * - [fontFamily]: "font_family" key — enum name string
 * - [biometricEnabled]: "biometric_enabled" key — app lock on/off (data-model.md §2; reused, not renamed)
 * - [appLockTimeout]: "app_lock_timeout" key — a `LockTimeout` option id
 *   (`immediate`/`after_1_min`/`after_5_min`/`after_15_min`, data-model.md §3), append-only
 * - [hideAmounts]: "hide_amounts" key — masks money on every surface (data-model.md §3)
 * - [notificationsMaster]: "notifications_master" key — app-wide alert switch (data-model.md §3)
 * - [syncEnabled]: "sync_enabled" key (stub — Phase 2)
 * - [assistantConsentGranted]: "assistant_consent_granted" key — durable across restart (FR-036,
 *   data-model.md §3); the assistant's own in-memory-only flag was the defect this replaces
 * - [geminiApiKey]: lives in the encrypted "secure_settings" DataStore, never in plaintext
 */
data class AppSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val accentColorHex: String = "#F05A28",
    val fontFamily: DhruvFont = DhruvFont.DEFAULT,
    val biometricEnabled: Boolean = false,
    val appLockTimeout: String = "after_1_min",
    val hideAmounts: Boolean = false,
    val notificationsMaster: Boolean = true,
    val syncEnabled: Boolean = false,
    val assistantConsentGranted: Boolean = false,
    val geminiApiKey: String? = null,
)
