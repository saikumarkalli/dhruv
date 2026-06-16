package com.dhruv.settings

import com.dhruv.core.ui.theme.AppTheme
import com.dhruv.core.ui.theme.DhruvFont

/**
 * Immutable snapshot of all user-configurable settings.
 *
 * Mapping from DataStore keys:
 * - [theme]: "dark_mode" key — "always_dark"↔DARK, "always_light"↔LIGHT, "system"↔SYSTEM
 * - [accentColorHex]: "accent_color_hex" key — Dhruv gold "#D4AF37" by default
 * - [fontFamily]: "font_family" key — enum name string
 * - [biometricEnabled]: "biometric_enabled" key
 * - [syncEnabled]: "sync_enabled" key (stub — Phase 2)
 * - [geminiApiKey]: lives in the encrypted "secure_settings" DataStore, never in plaintext
 */
data class AppSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val accentColorHex: String = "#D4AF37",
    val fontFamily: DhruvFont = DhruvFont.DEFAULT,
    val biometricEnabled: Boolean = false,
    val syncEnabled: Boolean = false,
    val geminiApiKey: String? = null
)
