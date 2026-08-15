package com.dhruv.finance.app.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhruv.core.navigation.NavTarget
import com.dhruv.core.navigation.TabKey
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.ui.FeatureHost
import com.dhruv.finance.app.navigation.NavigationDispatcher
import com.dhruv.finance.onboarding.ConsentScreen
import com.dhruv.finance.onboarding.EmptyStartScreen
import com.dhruv.finance.onboarding.OnboardingUiState
import com.dhruv.finance.onboarding.OnboardingViewModel
import com.dhruv.finance.onboarding.SignInScreen

/**
 * Top-level, pre-tab host for the onboarding flow (A2 sign-in -> A3 consent -> A4 empty start,
 * functional spec §5 Group A / registry §1: "bare, full-frame, no chrome"). `MainActivity` renders
 * this INSTEAD OF `AppShell` while `showOnboarding` is true (Task 3 decision 2) — this composable
 * never touches the pager/bottom-nav/tab machinery, only [OnboardingViewModel.uiState].
 *
 * Internal navigation between the three screens is a plain `when` over [OnboardingUiState], the
 * same shape `TabsScaffold`'s `when (tabs[page])` already uses — no `NavHost`/`NavController`
 * here: onboarding is a strictly linear, ViewModel-state-driven 3-screen flow with no back-stack or
 * deep-link need (Task 3 decision 3).
 *
 * Onboarding has no feature flag (it is a pre-tab shell concern, not a toggleable feature), so
 * [FeatureHost]'s `isEnabled` is hardcoded `true` per the global constraint — only a real
 * [OnboardingViewModel.featureError] can replace this content with an error card.
 */
@Composable
fun OnboardingHost(
    onboardingViewModel: OnboardingViewModel,
    crashReporter: CrashReporter,
    navigationDispatcher: NavigationDispatcher,
    modifier: Modifier = Modifier,
) {
    val uiState by onboardingViewModel.uiState.collectAsStateWithLifecycle()
    val featureError by onboardingViewModel.featureError.collectAsStateWithLifecycle()

    FeatureHost("onboarding", isEnabled = true, featureError = featureError, crashReporter = crashReporter) {
        when (val state = uiState) {
            is OnboardingUiState.SignIn ->
                SignInScreen(
                    onGoogleIdTokenReceived = onboardingViewModel::onGoogleIdTokenReceived,
                    onUseOfflineSelected = onboardingViewModel::onUseOfflineSelected,
                    modifier = modifier,
                )

            is OnboardingUiState.Consent ->
                ConsentScreen(
                    uiState = state,
                    onSwitchToggled = onboardingViewModel::onConsentSwitchToggled,
                    onContinue = onboardingViewModel::onConsentContinue,
                    modifier = modifier,
                )

            is OnboardingUiState.EmptyStart ->
                EmptyStartScreen(
                    // D6 Accounts / C4 Add holding don't exist yet (Phase 2/3) — dispatching
                    // toward their eventual owner tab (Money/Home) is inert until AppShell is
                    // reachable from here, exactly like EmptyStartScreen's own doc comment states.
                    onAddAccount = { navigationDispatcher.navigate(NavTarget.SelectTab(TabKey.MONEY)) },
                    onRecordWhatYouOwn = { navigationDispatcher.navigate(NavTarget.SelectTab(TabKey.HOME)) },
                    // Fix 1 (final whole-branch review, Critical) — A4's only actual exit; see
                    // EmptyStartScreen's and OnboardingViewModel.onSkipEmptyStart's doc comments.
                    onSkipEmptyStart = onboardingViewModel::onSkipEmptyStart,
                    modifier = modifier,
                )
        }
    }
}
