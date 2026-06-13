package com.dhruv.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AppDimens(
    val screenPadding: Dp = 16.dp,
    val cardPadding: Dp = 16.dp,
    val spacerHeightSmall: Dp = 8.dp,
    val spacerHeightMedium: Dp = 16.dp,
    val spacerHeightLarge: Dp = 24.dp,
    val calculatorKeyHeight: Dp = 54.dp,
    val isTabletOrLandscape: Boolean = false,
    val gridVerticalGap: Dp = 8.dp
)

data class AppTypographyResponsive(
    val displayLarge: TextUnit = 40.sp,
    val displayMedium: TextUnit = 32.sp,
    val headlineLarge: TextUnit = 28.sp,
    val headlineMedium: TextUnit = 20.sp,
    val titleLarge: TextUnit = 20.sp,
    val titleMedium: TextUnit = 16.sp,
    val bodyLarge: TextUnit = 16.sp,
    val bodyMedium: TextUnit = 14.sp,
    val bodySmall: TextUnit = 12.sp,
    val labelSmall: TextUnit = 11.sp
)

val LocalAppDimens = staticCompositionLocalOf { AppDimens() }
val LocalAppTypography = staticCompositionLocalOf { AppTypographyResponsive() }

object ResponsiveApp {
    val dimens: AppDimens
        @Composable
        @ReadOnlyComposable
        get() = LocalAppDimens.current

    val typography: AppTypographyResponsive
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTypography.current
}

fun calculateResponsiveMetrics(widthDp: Int, heightDp: Int): Pair<AppDimens, AppTypographyResponsive> {
    val isTablet = widthDp >= 600
    val isLandscape = widthDp > heightDp
    val isExtremelySmall = widthDp < 360 || heightDp < 600

    val dimens = when {
        isExtremelySmall -> AppDimens(
            screenPadding = 10.dp,
            cardPadding = 10.dp,
            spacerHeightSmall = 4.dp,
            spacerHeightMedium = 10.dp,
            spacerHeightLarge = 16.dp,
            calculatorKeyHeight = 44.dp,
            isTabletOrLandscape = false,
            gridVerticalGap = 4.dp
        )
        isTablet || (widthDp >= 720) -> AppDimens(
            screenPadding = 20.dp,
            cardPadding = 20.dp,
            spacerHeightSmall = 10.dp,
            spacerHeightMedium = 20.dp,
            spacerHeightLarge = 28.dp,
            calculatorKeyHeight = 62.dp,
            isTabletOrLandscape = true,
            gridVerticalGap = 10.dp
        )
        isLandscape -> AppDimens(
            screenPadding = 12.dp,
            cardPadding = 12.dp,
            spacerHeightSmall = 6.dp,
            spacerHeightMedium = 12.dp,
            spacerHeightLarge = 18.dp,
            calculatorKeyHeight = 42.dp,
            isTabletOrLandscape = true,
            gridVerticalGap = 6.dp
        )
        else -> AppDimens(
            screenPadding = 16.dp,
            cardPadding = 16.dp,
            spacerHeightSmall = 8.dp,
            spacerHeightMedium = 16.dp,
            spacerHeightLarge = 24.dp,
            calculatorKeyHeight = 56.dp,
            isTabletOrLandscape = false,
            gridVerticalGap = 8.dp
        )
    }

    val typography = when {
        isExtremelySmall -> AppTypographyResponsive(
            displayLarge = 32.sp,
            displayMedium = 25.sp,
            headlineLarge = 21.sp,
            headlineMedium = 17.sp,
            titleLarge = 16.sp,
            titleMedium = 13.sp,
            bodyLarge = 13.sp,
            bodyMedium = 11.sp,
            bodySmall = 10.sp,
            labelSmall = 9.sp
        )
        isTablet || (widthDp >= 720) -> AppTypographyResponsive(
            displayLarge = 46.sp,
            displayMedium = 38.sp,
            headlineLarge = 32.sp,
            headlineMedium = 24.sp,
            titleLarge = 22.sp,
            titleMedium = 18.sp,
            bodyLarge = 18.sp,
            bodyMedium = 15.sp,
            bodySmall = 13.sp,
            labelSmall = 12.sp
        )
        else -> AppTypographyResponsive(
            displayLarge = 40.sp,
            displayMedium = 31.sp,
            headlineLarge = 27.sp,
            headlineMedium = 19.sp,
            titleLarge = 19.sp,
            titleMedium = 15.sp,
            bodyLarge = 15.sp,
            bodyMedium = 13.sp,
            bodySmall = 11.sp,
            labelSmall = 10.sp
        )
    }

    return Pair(dimens, typography)
}
