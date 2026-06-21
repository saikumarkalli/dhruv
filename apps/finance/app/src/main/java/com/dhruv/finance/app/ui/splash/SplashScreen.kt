package com.dhruv.finance.app.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.components.DhruvLogoWordmarkVertical
import com.dhruv.core.ui.theme.DhruvLogoBg
import com.dhruv.core.ui.theme.DhruvNavy

/**
 * Compose representation of the splash state — shown briefly before the Android 12+
 * SplashScreen API hands off to MainActivity. Useful as a loading overlay on older devices
 * or during first-launch initialisation before the first frame is ready.
 *
 * Colors come from [com.dhruv.core.ui.theme.Color] so the palette is defined in one place.
 */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DhruvLogoBg),
        contentAlignment = Alignment.Center
    ) {
        DhruvLogoWordmarkVertical(
            appName = "finance",
            logoSize = 140.dp,
            textColor = DhruvNavy
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4F6FA)
@Composable
private fun SplashScreenPreview() {
    SplashScreen()
}
