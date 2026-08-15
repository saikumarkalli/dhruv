package com.dhruv.finance.app.ui.shell

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.components.EmptyStateCard
import com.dhruv.core.ui.components.InitialsTile
import com.dhruv.core.ui.components.NxCard
import com.dhruv.core.ui.components.NxTopBar
import com.dhruv.core.ui.components.SectionLabel
import com.dhruv.core.ui.components.StatItem
import com.dhruv.core.ui.components.ThreeUpStatRow
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.DhruvTheme
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/**
 * `profile` ("Account") — DhruvNext §6.9
 * (`platform/DESIGN-SYSTEM.md`). The design spec draws
 * a fully signed-in identity: real avatar/name/email, an "Edit profile" action, a Sync section
 * ("Synced 4 min ago" + a second-device row), and a "Sign out" row. None of that exists today —
 * Supabase auth is a future phase (ADR-0014 §3: R2, not built), so there is no account, nothing
 * has ever synced, and there is no session to sign out of. Rendering the design's sample content
 * as-is would misrepresent working functionality that doesn't exist, so this screen deliberately
 * renders the honest subset only:
 * - an identity header stating there is no account (no fake name/email, no "Edit profile" action)
 * - a stats strip using placeholder/zero values (see comment at the call site below)
 * - a single explicit empty state instead of a Sync section, apps mini-list, and Sign-out row
 */
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    Column(modifier = modifier.fillMaxSize().background(colors.bg)) {
        NxTopBar(title = "Account", onBack = onBack)
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(DhruvNextSpacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
        ) {
            NxCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // "?" reads as an unknown/no identity, unlike a fabricated name's initials.
                    InitialsTile(name = "?", size = 64.dp)
                    Text(
                        text = "Not signed in",
                        modifier = Modifier.padding(top = 12.dp),
                        color = colors.tx,
                        fontSize = DhruvNextType.title,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Local device only — no account yet",
                        modifier = Modifier.padding(top = 4.dp),
                        color = colors.tx2,
                        fontSize = DhruvNextType.body,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            NxCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionLabel(text = "Account activity")
                    // Placeholder/zero values only. There is no signed-in account and no
                    // calculator-history/tracker repository wired to this screen yet — real
                    // counts land once that data-layer wiring exists (ADR-0014 §3 phase order).
                    // Do not call a repository here in the meantime.
                    ThreeUpStatRow(
                        modifier = Modifier.padding(top = 8.dp),
                        items =
                            listOf(
                                StatItem(label = "Calculations", value = "0"),
                                StatItem(label = "Saved plans", value = "0"),
                                StatItem(label = "With dhruv", value = "—"),
                            ),
                    )
                }
            }

            EmptyStateCard(message = "Account sync isn't set up yet")
        }
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ProfileScreenPreview() {
    DhruvTheme {
        ProfileScreen(onBack = {})
    }
}
