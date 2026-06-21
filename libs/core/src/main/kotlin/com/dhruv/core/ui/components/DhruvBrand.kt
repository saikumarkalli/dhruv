package com.dhruv.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dhruv.core.R
import com.dhruv.core.ui.theme.DhruvNavy
import com.dhruv.core.ui.theme.DhruvSilver
import com.dhruv.core.ui.theme.DhruvSilverLight
import com.dhruv.core.ui.theme.wordmarkStyle

/**
 * Dhruv crest icon, tinted to the given [tint] color.
 * Defaults to [DhruvSilverLight] for use on dark/navy surfaces.
 */
@Composable
fun DhruvCrest(
    modifier: Modifier = Modifier,
    tint: Color = DhruvSilverLight
) {
    Image(
        painter = painterResource(id = R.drawable.ic_dhruv_crest),
        contentDescription = "Dhruv crest",
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier
    )
}

/**
 * Full-color "hero" logo — the detailed chrome compass artwork, rendered untinted.
 * Use this large (≥72.dp) on light/neutral surfaces (splash, brand headers); for small
 * theme-adaptive UI marks use [DhruvCrest] instead.
 */
@Composable
fun DhruvLogo(
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.ic_dhruv_logo),
        contentDescription = "Dhruv logo",
        modifier = modifier
    )
}

/** Intrinsic aspect ratio (width / height) of the `ic_dhruv_wordmark` asset. */
private const val WORDMARK_ASPECT = 944f / 256f

/**
 * The master "dhruv" wordmark image, rendered at [height] with its aspect ratio preserved.
 * Use as the in-app top-left brand mark (app bar). Reads on both light and dark surfaces.
 */
@Composable
fun DhruvWordmarkImage(
    modifier: Modifier = Modifier,
    height: Dp = 26.dp
) {
    Image(
        painter = painterResource(id = R.drawable.ic_dhruv_wordmark),
        contentDescription = "dhruv",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .height(height)
            .aspectRatio(WORDMARK_ASPECT, matchHeightConstraintsFirst = true)
    )
}

/**
 * Horizontal lockup: the full-color [DhruvLogo] on the left, brand name text on the right.
 * Used as the in-app "main" brand mark (e.g. the top app bar). [textColor] defaults to
 * [DhruvNavy]; pass a theme color (e.g. `onSurface`) when the surface can be dark.
 */
@Composable
fun DhruvLogoWordmark(
    appName: String = "",
    modifier: Modifier = Modifier,
    logoSize: Dp = 32.dp,
    textColor: Color = DhruvNavy
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DhruvLogo(modifier = Modifier.size(logoSize))
        Text(
            text = if (appName.isBlank()) "dhruv" else "dhruv $appName",
            style = wordmarkStyle.copy(color = textColor)
        )
    }
}

/**
 * Vertical hero lockup: the full-color [DhruvLogo] above the brand name text.
 * Designed for the splash screen and the Settings brand header on a light surface
 * (defaults to [DhruvNavy] text for contrast on [com.dhruv.core.ui.theme.DhruvLogoBg]).
 */
@Composable
fun DhruvLogoWordmarkVertical(
    appName: String = "",
    modifier: Modifier = Modifier,
    logoSize: Dp = 96.dp,
    textColor: Color = DhruvNavy
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DhruvLogo(modifier = Modifier.size(logoSize))
        Text(
            text = if (appName.isBlank()) "dhruv" else "dhruv $appName",
            style = wordmarkStyle.copy(color = textColor)
        )
    }
}

/**
 * Horizontal wordmark: crest icon on the left, brand name text on the right.
 *
 * @param appName Optional sub-app name appended after "dhruv" (e.g. "finance").
 *                Pass an empty string (default) to render just "dhruv".
 */
@Composable
fun DhruvWordmark(
    appName: String = "",
    modifier: Modifier = Modifier,
    crestTint: Color = DhruvSilverLight,
    textColor: Color = DhruvSilver
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DhruvCrest(
            tint = crestTint,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = if (appName.isBlank()) "dhruv" else "dhruv $appName",
            style = wordmarkStyle.copy(color = textColor)
        )
    }
}

/**
 * Vertical wordmark: crest icon above the brand name text.
 *
 * @param appName Optional sub-app name appended after "dhruv" (e.g. "finance").
 *                Pass an empty string (default) to render just "dhruv".
 */
@Composable
fun DhruvWordmarkVertical(
    appName: String = "",
    modifier: Modifier = Modifier,
    crestTint: Color = DhruvSilverLight,
    textColor: Color = DhruvSilver
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DhruvCrest(
            tint = crestTint,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = if (appName.isBlank()) "dhruv" else "dhruv $appName",
            style = wordmarkStyle.copy(color = textColor)
        )
    }
}

/**
 * White silhouette crest for use as a notification small icon.
 * Renders the white-on-transparent variant — no tint applied.
 */
@Composable
fun DhruvNotificationIcon(
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.ic_dhruv_crest_white),
        contentDescription = null,
        modifier = modifier
    )
}
