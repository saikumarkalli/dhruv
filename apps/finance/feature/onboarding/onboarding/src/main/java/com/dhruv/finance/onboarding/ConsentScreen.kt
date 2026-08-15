package com.dhruv.finance.onboarding

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.dhruv.core.ui.components.NxButton
import com.dhruv.core.ui.components.NxCard
import com.dhruv.core.ui.components.SwitchRow
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.DhruvTheme
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import com.dhruv.finance.data.tracker.auth.ConsentState

/**
 * A3 — DPDP consent (functional spec §5 Group A). Bare, full-frame, no chrome (registry §1).
 * Three independently-revocable switches ([ConsentSwitch]) plus a fourth, non-toggleable
 * retention/erasure info block — [OnboardingConfig.consentSwitchCopy] fixes the display order.
 * Every switch persists immediately through [OnboardingViewModel.onConsentSwitchToggled]
 * (ONB-BR-004/005); "Continue" proceeds regardless of what's toggled (ONB-BR-002).
 */
@Composable
fun ConsentScreen(
    uiState: OnboardingUiState.Consent,
    onSwitchToggled: (ConsentSwitch, Boolean) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(DhruvNextSpacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
    ) {
        OnboardingHeader()

        Text(
            text = OnboardingConfig.CONSENT_HEADER,
            color = colors.tx,
            fontSize = DhruvNextType.title,
            fontWeight = FontWeight.Bold,
        )

        NxCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                OnboardingConfig.consentSwitchCopy.forEachIndexed { index, copy ->
                    SwitchRow(
                        label = copy.label,
                        description = copy.scopeStatement,
                        checked = uiState.switches.isChecked(copy.switch),
                        onCheckedChange = { onSwitchToggled(copy.switch, it) },
                    )
                    if (index != OnboardingConfig.consentSwitchCopy.lastIndex) {
                        HorizontalDivider(color = colors.line2)
                    }
                }
            }
        }

        NxCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = OnboardingConfig.CONSENT_RETENTION_TITLE,
                    color = colors.tx,
                    fontSize = DhruvNextType.cardTitle,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = OnboardingConfig.CONSENT_RETENTION_BODY,
                    modifier = Modifier.padding(top = DhruvNextSpacing.inputGroupGap),
                    color = colors.tx2,
                    fontSize = DhruvNextType.meta,
                )
            }
        }

        NxButton(
            text = OnboardingConfig.CONSENT_CONTINUE_CTA,
            enabled = !uiState.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
            onClick = onContinue,
        )
    }
}

/** Maps a [ConsentSwitch] case to its current on/off value in [ConsentState]. */
private fun ConsentState.isChecked(switch: ConsentSwitch): Boolean =
    when (switch) {
        ConsentSwitch.SYNC_FINANCIAL_RECORDS -> syncFinancialRecords
        ConsentSwitch.READ_TRANSACTION_SMS -> readTransactionSms
        ConsentSwitch.ASK_DHRUV_ABOUT_MONEY -> askDhruvAboutMoney
    }

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ConsentScreenPreview() {
    DhruvTheme {
        ConsentScreen(
            uiState = OnboardingUiState.Consent(switches = ConsentState(), isSubmitting = false),
            onSwitchToggled = { _, _ -> },
            onContinue = {},
        )
    }
}
