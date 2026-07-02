package com.dhruv.core.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily

@Composable
fun Modifier.appGradientBackground(): Modifier {
    val isDark = MaterialTheme.colorScheme.background != Color.White
    val primary = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background

    return if (isDark) {
        this.background(
            brush =
                Brush.linearGradient(
                    colors =
                        listOf(
                            primary.copy(alpha = 0.12f),
                            background.copy(alpha = 0.98f),
                            background,
                        ),
                ),
        )
    } else {
        this.background(
            brush =
                Brush.linearGradient(
                    colors =
                        listOf(
                            primary.copy(alpha = 0.05f),
                            background,
                            background,
                        ),
                ),
        )
    }
}

private val DarkColorScheme =
    darkColorScheme(
        primary = PrimaryDark,
        secondary = SecondaryDark,
        tertiary = TertiaryDark,
        background = BackgroundDark,
        surface = SurfaceDark,
        surfaceVariant = SurfaceVariantDark,
        onPrimary = Color(0xFF003258),
        onSecondary = Color(0xFF161C24),
        onTertiary = Color(0xFF211047),
        onBackground = OnSurfaceDark,
        onSurface = OnSurfaceDark,
        onSurfaceVariant = OnSurfaceVariantDark,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = PrimaryLight,
        secondary = SecondaryLight,
        tertiary = TertiaryLight,
        background = BackgroundLight,
        surface = SurfaceLight,
        surfaceVariant = Color(0xFFE5E7EB),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = Color(0xFF111827),
        onSurface = Color(0xFF111827),
        onSurfaceVariant = Color(0xFF374151),
    )

/**
 * Primary DhruvTheme overload used by the Finance app and driven by [AppSettings].
 * - [theme] maps to the global dark/light/system preference.
 * - [accentColorHex] optionally overrides [MaterialTheme.colorScheme.primary] with a custom hex
 *   string in "#RRGGBB" format (Dhruv-gold default "#D4AF37" when null).
 * - [font] selects the global [FontFamily] (DEFAULT→Default, MONO→Monospace, ROUNDED→SansSerif).
 */
@Composable
fun DhruvTheme(
    theme: AppTheme = AppTheme.SYSTEM,
    accentColorHex: String? = null,
    font: DhruvFont = DhruvFont.DEFAULT,
    content: @Composable () -> Unit,
) {
    val darkTheme =
        when (theme) {
            AppTheme.DARK -> true
            AppTheme.LIGHT -> false
            AppTheme.SYSTEM -> isSystemInDarkTheme()
        }

    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val colorScheme =
        if (accentColorHex != null) {
            val accent =
                runCatching { Color(android.graphics.Color.parseColor(accentColorHex)) }
                    .getOrElse { baseScheme.primary }
            baseScheme.copy(primary = accent, secondary = accent)
        } else {
            baseScheme
        }

    val fontFamily =
        when (font) {
            DhruvFont.DEFAULT -> FontFamily.Default
            DhruvFont.MONO -> FontFamily.Monospace
            DhruvFont.ROUNDED -> FontFamily.SansSerif // placeholder — real rounded font is a follow-up
            DhruvFont.BRAND_SERIF -> brandSerifFamily
        }
    val resolvedTypography =
        if (font == DhruvFont.DEFAULT) {
            Typography
        } else {
            Typography.copy(
                displayLarge = Typography.displayLarge.copy(fontFamily = fontFamily),
                displayMedium = Typography.displayMedium.copy(fontFamily = fontFamily),
                displaySmall = Typography.displaySmall.copy(fontFamily = fontFamily),
                headlineLarge = Typography.headlineLarge.copy(fontFamily = fontFamily),
                headlineMedium = Typography.headlineMedium.copy(fontFamily = fontFamily),
                headlineSmall = Typography.headlineSmall.copy(fontFamily = fontFamily),
                titleLarge = Typography.titleLarge.copy(fontFamily = fontFamily),
                titleMedium = Typography.titleMedium.copy(fontFamily = fontFamily),
                titleSmall = Typography.titleSmall.copy(fontFamily = fontFamily),
                bodyLarge = Typography.bodyLarge.copy(fontFamily = fontFamily),
                bodyMedium = Typography.bodyMedium.copy(fontFamily = fontFamily),
                bodySmall = Typography.bodySmall.copy(fontFamily = fontFamily),
                labelLarge = Typography.labelLarge.copy(fontFamily = fontFamily),
                labelMedium = Typography.labelMedium.copy(fontFamily = fontFamily),
                labelSmall = Typography.labelSmall.copy(fontFamily = fontFamily),
            )
        }

    val configuration = LocalConfiguration.current
    val (dimens, responsiveType) =
        calculateResponsiveMetrics(
            configuration.screenWidthDp,
            configuration.screenHeightDp,
        )

    CompositionLocalProvider(
        LocalAppDimens provides dimens,
        LocalAppTypography provides responsiveType,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = resolvedTypography,
            content = content,
        )
    }
}

/**
 * Legacy overload — kept for existing callers that pass [darkModePreference] as a raw string.
 * Delegates to the enum-based overload.
 */
@Composable
fun DhruvTheme(
    darkModePreference: String = "system",
    content: @Composable () -> Unit,
) {
    val theme =
        when (darkModePreference) {
            "always_dark" -> AppTheme.DARK
            "always_light" -> AppTheme.LIGHT
            else -> AppTheme.SYSTEM
        }
    DhruvTheme(theme = theme, accentColorHex = null, font = DhruvFont.DEFAULT, content = content)
}

@Composable
fun SectionTheme(
    colorPreference: String,
    darkModePreference: String,
    content: @Composable () -> Unit,
) {
    val darkTheme =
        when (darkModePreference) {
            "always_dark" -> true
            "always_light" -> false
            else -> isSystemInDarkTheme()
        }

    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val accentColor = getAccentColor(colorPreference, darkTheme)

    val coloredScheme =
        baseScheme.copy(
            primary = accentColor,
            secondary = if (darkTheme) accentColor else accentColor,
        )

    val configuration = LocalConfiguration.current
    val (dimens, responsiveType) =
        calculateResponsiveMetrics(
            configuration.screenWidthDp,
            configuration.screenHeightDp,
        )

    CompositionLocalProvider(
        LocalAppDimens provides dimens,
        LocalAppTypography provides responsiveType,
    ) {
        MaterialTheme(
            colorScheme = coloredScheme,
            typography = Typography,
            content = content,
        )
    }
}
