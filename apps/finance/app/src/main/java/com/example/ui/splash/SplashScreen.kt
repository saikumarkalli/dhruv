package com.example.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dhruv.core.ui.components.DhruvWordmark
import com.dhruv.core.ui.theme.DhruvNavy
import com.dhruv.core.ui.theme.DhruvSilver
import com.dhruv.core.ui.theme.DhruvSilverLight

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
            .background(DhruvNavy),
        contentAlignment = Alignment.Center
    ) {
        DhruvWordmark(
            appName = "finance",
            crestTint = DhruvSilverLight,
            textColor = DhruvSilver
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1B2A)
@Composable
private fun SplashScreenPreview() {
    SplashScreen()
}
