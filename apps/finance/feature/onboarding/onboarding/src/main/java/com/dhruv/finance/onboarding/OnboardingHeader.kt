package com.dhruv.finance.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.components.DhruvWordmarkImage
import com.dhruv.core.ui.theme.DhruvNextSpacing

/**
 * The one brand mark every onboarding screen (A2/A3/A4) shows at the top, in the same size and
 * position — found missing live: A3/ConsentScreen and A4/EmptyStartScreen had no wordmark at all,
 * so the flow read as inconsistent step-to-step even though A2/SignInScreen already had one (at a
 * much larger, "hero" size that wouldn't fit above A3/A4's own content without crowding it).
 * [DhruvWordmarkImage] "reads on both light and dark surfaces" per its own doc comment, so this
 * needs no theme-specific tinting logic.
 */
@Composable
fun OnboardingHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // WORDMARK_HEIGHT sits between the splash's hero size (56.dp) and the persistent top-bar's
        // small size (26.dp, MainActivity.kt) — big enough to read as a real header, small enough to
        // leave A3/A4's content room. Found live (2026-08-15): this previously read
        // `DhruvNextSpacing.sectionGap` (24.dp) — a spacing token, not a size, and *smaller* than
        // even the top-bar mark — so it rendered as a near-invisible sliver flush against the status
        // bar instead of a header. Dedicated dp constant, matching the sibling precedent
        // (HERO_WORDMARK_HEIGHT/TOP_BAR_WORDMARK_HEIGHT in SplashScreen.kt/MainActivity.kt) —
        // wordmark height is a component-specific size, not a layout spacing gap.
        DhruvWordmarkImage(
            height = WORDMARK_HEIGHT,
            modifier = Modifier.padding(bottom = DhruvNextSpacing.sectionGap),
        )
    }
}

private val WORDMARK_HEIGHT = 40.dp
