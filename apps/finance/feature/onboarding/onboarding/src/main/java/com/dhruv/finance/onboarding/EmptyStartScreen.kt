package com.dhruv.finance.onboarding

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import com.dhruv.core.ui.components.ListGroup
import com.dhruv.core.ui.components.ListGroupRow
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.DhruvTheme
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/**
 * A4 — empty start (functional spec §5 Group A). Bare, full-frame, no chrome (registry §1). Day
 * one, exactly two tasks (no dashboard chrome, no zeros-everywhere dashboard) plus an "Import a
 * CSV" escape hatch that is deliberately a disabled stub (ONB-BR-007 — deferred, no CSV mapper
 * built here).
 *
 * [onAddAccount]/[onRecordWhatYouOwn] are plain callbacks — `OnboardingHost` wires them to the
 * real cross-tab `NavigationDispatcher`/`NavTarget.SelectTab` mechanism, dispatching toward the tab
 * that will eventually own D6 Accounts / C4 Add holding (Money and Home respectively, per the
 * surface registry). `AppShell` isn't mounted underneath onboarding in this phase, so the dispatch
 * is inert today — wiring it now means no rework is needed once a later phase lets A4 hand off to
 * the shell.
 *
 * [onSkipEmptyStart] (Fix 1 — final whole-branch review, Critical) is the screen's only actual
 * exit: without it, a signed-in user landed here permanently, since [onAddAccount]/
 * [onRecordWhatYouOwn]'s dispatch is inert (above) and there is no `BackHandler` anywhere in the
 * app. Secondary/text-style, below the two primary tasks and the disabled CSV stub — mirrors A2
 * `SignInScreen`'s "Use offline" `TextButton`.
 */
@Composable
fun EmptyStartScreen(
    onAddAccount: () -> Unit,
    onRecordWhatYouOwn: () -> Unit,
    onSkipEmptyStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current

    Column(
        modifier = modifier.fillMaxSize().padding(DhruvNextSpacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
    ) {
        OnboardingHeader()

        ListGroup(
            rows =
                listOf(
                    {
                        ListGroupRow(
                            title = OnboardingConfig.EMPTY_START_TASK_1_TITLE,
                            icon = Icons.Default.AccountBalanceWallet,
                            onClick = onAddAccount,
                        )
                    },
                    {
                        ListGroupRow(
                            title = OnboardingConfig.EMPTY_START_TASK_2_TITLE,
                            icon = Icons.Default.Savings,
                            onClick = onRecordWhatYouOwn,
                        )
                    },
                    {
                        // ONB-BR-007: deferred, visibly disabled — no onClick, muted opacity, no chevron.
                        ListGroupRow(
                            title = OnboardingConfig.EMPTY_START_CSV_CTA,
                            subtitle = OnboardingConfig.EMPTY_START_CSV_SUBTITLE,
                            icon = Icons.Default.UploadFile,
                            showChevron = false,
                            onClick = null,
                            modifier = Modifier.alpha(DISABLED_ROW_ALPHA),
                        )
                    },
                ),
        )

        TextButton(
            onClick = onSkipEmptyStart,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(
                text = OnboardingConfig.EMPTY_START_SKIP_CTA,
                color = colors.tx2,
                fontSize = DhruvNextType.body,
            )
        }
    }
}

private const val DISABLED_ROW_ALPHA = 0.5f

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun EmptyStartScreenPreview() {
    DhruvTheme {
        EmptyStartScreen(onAddAccount = {}, onRecordWhatYouOwn = {}, onSkipEmptyStart = {})
    }
}
