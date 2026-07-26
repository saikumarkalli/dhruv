package com.dhruv.finance.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.dhruv.core.ui.theme.ColorOptions
import java.util.Locale

/**
 * Appearance section row content (ADR-0024 / DhruvNext §6.9). Previously this file hosted a
 * `ModalBottomSheet` ("Appearance & Colors") with a per-domain `SectionTheme` color picker per
 * finance/converter/date/time section (ADR-0014 §8) — that whole model is retired. Appearance is
 * now inline in the main Settings body: a Theme segmented row, ONE global 4-swatch accent picker
 * (ADR-0024 decision 2), and a Material-You placeholder — each row below is composed directly into
 * [com.dhruv.core.ui.components.ListGroup]'s `rows` list by [SettingsScreen].
 */

private val THEME_OPTIONS = listOf("system" to "System", "always_light" to "Light", "always_dark" to "Dark")

/** Theme row — reuses the existing [com.dhruv.settings.SettingsRepository] dark-mode preference flow. */
@Composable
fun AppearanceThemeRow(
    darkModePreference: String,
    onThemeChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = THEME_OPTIONS.indexOfFirst { it.first == darkModePreference }.takeIf { it >= 0 } ?: 0
    LabeledSegmentedRow(
        label = "Theme",
        options = THEME_OPTIONS.map { it.second },
        selectedIndex = selectedIndex,
        onSelected = { index -> onThemeChanged(THEME_OPTIONS[index].first) },
        modifier = modifier,
    )
}

/**
 * The 4 global-accent swatches, sourced from [ColorOptions]' "orange"/"green"/"blue"/"purple"
 * entries (their `lightPrimary` hex — the values that already ship in this codebase; note this is
 * numerically distinct from [com.dhruv.settings.AppSettings.accentColorHex]'s own default
 * "#F05A28", so the default accent won't show any swatch pre-selected until the user picks one).
 */
fun defaultAccentSwatches(): List<AccentSwatch> =
    listOf("orange", "green", "blue", "purple").mapNotNull { id ->
        ColorOptions.firstOrNull { it.id == id }?.let { option ->
            AccentSwatch(
                id = option.id,
                label = option.name,
                hex = option.lightPrimary.toHexString(),
                color = option.lightPrimary,
            )
        }
    }

/**
 * "#RRGGBB" formatting without [android.graphics.Color.parseColor]/`toArgb()` — deliberately
 * hand-rolled from the [Color] channel floats (same reasoning as `DhruvNextTokens.parseHexColor`'s
 * own doc comment: Android-framework color stubs return black under plain JVM unit tests, no
 * Robolectric here).
 */
private fun Color.toHexString(): String {
    val r = (red * MAX_CHANNEL_F + HALF_CHANNEL).toInt().coerceIn(0, MAX_CHANNEL_I)
    val g = (green * MAX_CHANNEL_F + HALF_CHANNEL).toInt().coerceIn(0, MAX_CHANNEL_I)
    val b = (blue * MAX_CHANNEL_F + HALF_CHANNEL).toInt().coerceIn(0, MAX_CHANNEL_I)
    return String.format(Locale.US, "#%02X%02X%02X", r, g, b)
}

private const val MAX_CHANNEL_F = 255f
private const val MAX_CHANNEL_I = 255
private const val HALF_CHANNEL = 0.5f
