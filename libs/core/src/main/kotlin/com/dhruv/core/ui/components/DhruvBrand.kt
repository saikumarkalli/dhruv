package com.dhruv.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dhruv.core.R
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
