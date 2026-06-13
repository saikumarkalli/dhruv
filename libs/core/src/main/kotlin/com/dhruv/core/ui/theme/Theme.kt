package com.dhruv.core.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun Modifier.appGradientBackground(): Modifier {
    val isDark = MaterialTheme.colorScheme.background != Color.White
    val primary = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background
    
    return if (isDark) {
        this.background(
            brush = Brush.linearGradient(
                colors = listOf(
                    primary.copy(alpha = 0.12f),
                    background.copy(alpha = 0.98f),
                    background
                )
            )
        )
    } else {
        this.background(
            brush = Brush.linearGradient(
                colors = listOf(
                    primary.copy(alpha = 0.05f),
                    background,
                    background
                )
            )
        )
    }
}


private val DarkColorScheme = darkColorScheme(
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
    onSurfaceVariant = OnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
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
    onSurfaceVariant = Color(0xFF374151)
)

@Composable
fun DhruvTheme(
    darkModePreference: String = "system",
    content: @Composable () -> Unit
) {
    val darkTheme = when (darkModePreference) {
        "always_dark" -> true
        "always_light" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val configuration = LocalConfiguration.current
    val (dimens, responsiveType) = calculateResponsiveMetrics(
        configuration.screenWidthDp,
        configuration.screenHeightDp
    )

    CompositionLocalProvider(
        LocalAppDimens provides dimens,
        LocalAppTypography provides responsiveType
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun SectionTheme(
    colorPreference: String,
    darkModePreference: String,
    content: @Composable () -> Unit
) {
    val darkTheme = when (darkModePreference) {
        "always_dark" -> true
        "always_light" -> false
        else -> isSystemInDarkTheme()
    }

    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val accentColor = getAccentColor(colorPreference, darkTheme)

    val coloredScheme = baseScheme.copy(
        primary = accentColor,
        secondary = if (darkTheme) accentColor else accentColor
    )

    val configuration = LocalConfiguration.current
    val (dimens, responsiveType) = calculateResponsiveMetrics(
        configuration.screenWidthDp,
        configuration.screenHeightDp
    )

    CompositionLocalProvider(
        LocalAppDimens provides dimens,
        LocalAppTypography provides responsiveType
    ) {
        MaterialTheme(
            colorScheme = coloredScheme,
            typography = Typography,
            content = content
        )
    }
}
