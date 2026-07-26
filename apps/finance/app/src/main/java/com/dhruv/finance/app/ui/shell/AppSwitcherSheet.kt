package com.dhruv.finance.app.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.core.ui.components.DhruvModalSheet
import com.dhruv.core.ui.components.ListGroup
import com.dhruv.core.ui.components.ListGroupRow
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/** Disabled-row opacity — MD disabled-state guidance (0.38–0.5 reduced emphasis). */
private const val DISABLED_ALPHA = 0.5f

/**
 * `shell` — the app-switcher bottom sheet (DhruvNext §6.9
 * (`docs/superpowers/specs/2026-07-25-dhruvnext-ui-ux-design-reference.md`); app roster per
 * `platform/PLATFORM.md` §1). Finance is the only shipped app today; Tools and Vault are real
 * modules already named in the platform's own app table ("planned" / "future" respectively) — not
 * invented destinations — so they render disabled with a "SOON" badge and no `onClick`, rather
 * than a fake navigation target.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSwitcherSheet(onDismiss: () -> Unit) {
    val colors = LocalDhruvNextColors.current
    DhruvModalSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DhruvNextSpacing.screenGutter)
                    .padding(bottom = DhruvNextSpacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
        ) {
            Text(
                text = "Your dhruv apps — one account, one sync, separate vaults.",
                color = colors.tx2,
                fontSize = 13.sp,
            )
            ListGroup(
                rows =
                    listOf(
                        {
                            ListGroupRow(
                                title = "Finance",
                                subtitle = "Net worth, calculators, planners",
                                icon = Icons.Default.AccountBalanceWallet,
                                showChevron = false,
                                onClick = onDismiss,
                                trailing = { AppStatusBadge(text = "OPEN", active = true) },
                            )
                        },
                        {
                            ListGroupRow(
                                modifier = Modifier.alpha(DISABLED_ALPHA),
                                title = "Tools",
                                subtitle = "Notes, clipboard, timer, QR — planned",
                                icon = Icons.Default.Build,
                                showChevron = false,
                                onClick = null,
                                trailing = { AppStatusBadge(text = "SOON", active = false) },
                            )
                        },
                        {
                            ListGroupRow(
                                modifier = Modifier.alpha(DISABLED_ALPHA),
                                title = "Vault",
                                subtitle = "Password manager, E2E-encrypted — future",
                                icon = Icons.Default.Lock,
                                showChevron = false,
                                onClick = null,
                                trailing = { AppStatusBadge(text = "SOON", active = false) },
                            )
                        },
                    ),
            )
        }
    }
}

/** Small tonal pill — "OPEN" (current app, accent-tinted) vs "SOON" (planned/future, neutral). */
@Composable
private fun AppStatusBadge(
    text: String,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(if (active) colors.accSoft else colors.surf2)
                .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            color = if (active) colors.acc else colors.tx3,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
