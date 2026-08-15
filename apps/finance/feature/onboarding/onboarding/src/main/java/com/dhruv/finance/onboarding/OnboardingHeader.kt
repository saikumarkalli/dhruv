package com.dhruv.finance.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        // Between A2's old hero size (sectionGap * 2) and the top-bar's small size (26.dp) — big
        // enough to read as a real header, small enough to leave A3/A4's content room.
        DhruvWordmarkImage(
            height = DhruvNextSpacing.sectionGap,
            modifier = Modifier.padding(bottom = DhruvNextSpacing.sectionGap),
        )
    }
}
