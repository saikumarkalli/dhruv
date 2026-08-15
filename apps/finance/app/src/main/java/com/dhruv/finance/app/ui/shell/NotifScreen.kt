package com.dhruv.finance.app.ui.shell

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dhruv.core.ui.components.EmptyStateCard
import com.dhruv.core.ui.components.NxTopBar
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvTheme
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/**
 * `notif` ("Notifications") — DhruvNext §6.9
 * (`platform/DESIGN-SYSTEM.md`). The design spec shows
 * date-grouped notification cards (an EMI-due reminder, a budget breach, a health-score change, a
 * backup confirmation) — but nothing in this app produces a notification yet; bill/budget
 * reminders and backup jobs are future phases (R4/R6), not built. Fabricating sample notification
 * cards here would misrepresent a feature that doesn't exist, so this screen is the honest empty
 * state until a real notification source is wired up.
 */
@Composable
fun NotifScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    Column(modifier = modifier.fillMaxSize().background(colors.bg)) {
        NxTopBar(title = "Notifications", onBack = onBack)
        Box(
            modifier = Modifier.fillMaxSize().padding(DhruvNextSpacing.screenGutter),
            contentAlignment = Alignment.Center,
        ) {
            EmptyStateCard(
                message = "You're all caught up — no notifications yet",
                icon = Icons.Default.NotificationsNone,
            )
        }
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun NotifScreenPreview() {
    DhruvTheme {
        NotifScreen(onBack = {})
    }
}
