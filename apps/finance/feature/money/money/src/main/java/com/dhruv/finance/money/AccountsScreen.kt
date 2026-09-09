package com.dhruv.finance.money

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhruv.core.format.Paise
import com.dhruv.core.ui.components.DisclaimerFooter
import com.dhruv.core.ui.components.EmptyStateCard
import com.dhruv.core.ui.components.ListGroup
import com.dhruv.core.ui.components.ListGroupRow
import com.dhruv.core.ui.components.MoneyText
import com.dhruv.core.ui.components.MoneyTextVariant
import com.dhruv.core.ui.components.NxButton
import com.dhruv.core.ui.components.NxButtonSize
import com.dhruv.core.ui.components.OfflineStateCard
import com.dhruv.core.ui.components.ProgressRing
import com.dhruv.core.ui.components.RetryErrorCard
import com.dhruv.core.ui.components.SectionLabel
import com.dhruv.core.ui.components.SignedOutCard
import com.dhruv.core.ui.components.SkeletonBlock
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import com.dhruv.finance.data.tracker.model.Account
import com.dhruv.finance.money.MoneyConfig.accountTypeLabels

/**
 * D6 (accounts list, US3) — one honest "spendable now" total, credit shown separately as owed-not-
 * held (FR-017/FR-018), and the per-account staleness note (FR-020). Full screen-state set per
 * FR-032 (T047).
 */
@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel,
    onOpenAccount: (String) -> Unit,
    onAddAccount: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Found 2026-09-05, live-device audit: D6 only ever loaded once (`AccountsViewModel.init`),
    // so a freshly created account (D6a "Add account" pushes a separate route and pops back) was
    // invisible until the whole tab's ViewModel was torn down and recreated. This screen
    // recomposes fresh every time D6 is navigated back to (NavHost only composes the current
    // back-stack entry), so re-running `load()` here is the same "reload on return" fix
    // TransactionDetailScreen already uses for its own `LaunchedEffect(transactionId)`.
    LaunchedEffect(Unit) { viewModel.load() }

    Box(modifier = modifier.fillMaxSize()) {
        when (val current = state) {
            is AccountsUiState.Loading -> SkeletonBlock(modifier = Modifier.fillMaxSize())
            is AccountsUiState.Error ->
                RetryErrorCard(
                    message = current.message,
                    onRetry = { viewModel.load() },
                    modifier = Modifier.padding(DhruvNextSpacing.screenGutter),
                )
            is AccountsUiState.Offline ->
                OfflineStateCard(onRetry = { viewModel.load() }, modifier = Modifier.padding(DhruvNextSpacing.screenGutter))
            is AccountsUiState.SignedOut ->
                SignedOutCard(
                    message = "Sign in to see your accounts.",
                    actionLabel = "Sign in",
                    onAction = onSignIn,
                    modifier = Modifier.padding(DhruvNextSpacing.screenGutter),
                )
            is AccountsUiState.Loaded ->
                if (current.isEmpty) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                        EmptyStateCard(
                            message = "Add your first account",
                            modifier = Modifier.padding(horizontal = DhruvNextSpacing.screenGutter),
                        )
                        NxButton(
                            text = "Add account",
                            onClick = onAddAccount,
                            block = true,
                            modifier =
                                Modifier
                                    .padding(top = 12.dp)
                                    .padding(horizontal = DhruvNextSpacing.screenGutter),
                        )
                    }
                } else {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(DhruvNextSpacing.screenGutter),
                    ) {
                        SpendableNowCard(spendableNowPaise = current.spendableNowPaise, onAddAccount = onAddAccount)

                        AccountGroup(
                            label = "Bank",
                            accounts = current.bankAccounts,
                            onOpenAccount = onOpenAccount,
                        )
                        AccountGroup(
                            label = "Cash · Wallet",
                            accounts = current.cashAndWalletAccounts,
                            onOpenAccount = onOpenAccount,
                        )
                        CreditAccountGroup(accounts = current.creditAccounts, onOpenAccount = onOpenAccount)

                        DisclaimerFooter(
                            text = "Automatic balance refresh arrives with account linking.",
                            modifier = Modifier.padding(top = DhruvNextSpacing.sectionGap),
                        )
                    }
                }
        }
    }
}

@Composable
private fun SpendableNowCard(
    spendableNowPaise: Long,
    onAddAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DhruvNextRadii.card))
                .background(colors.surf2)
                .padding(DhruvNextSpacing.cardPadding),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SectionLabel(text = "Spendable now")
            NxButton(text = "Add account", onClick = onAddAccount, size = NxButtonSize.Small)
        }
        MoneyText(
            paise = spendableNowPaise,
            variant = MoneyTextVariant.Hero,
            color = colors.tx,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun AccountGroup(
    label: String,
    accounts: List<Account>,
    onOpenAccount: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (accounts.isEmpty()) return
    Column(modifier = modifier.padding(top = DhruvNextSpacing.sectionGap)) {
        SectionLabel(text = label, modifier = Modifier.padding(bottom = 8.dp))
        ListGroup(
            rows =
                accounts.map { account ->
                    {
                        ListGroupRow(
                            title = account.name,
                            subtitle = accountSubtitle(account),
                            onClick = { onOpenAccount(account.id) },
                            trailing = {
                                MoneyText(
                                    paise = account.balancePaise ?: account.openingBalancePaise,
                                    color = LocalDhruvNextColors.current.tx,
                                )
                            },
                        )
                    }
                },
        )
    }
}

@Composable
private fun CreditAccountGroup(
    accounts: List<Account>,
    onOpenAccount: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (accounts.isEmpty()) return
    Column(modifier = modifier.padding(top = DhruvNextSpacing.sectionGap)) {
        SectionLabel(text = stringResource(R.string.money_accounts_credit_group_label), modifier = Modifier.padding(bottom = 8.dp))
        ListGroup(
            rows = accounts.map { account -> { CreditAccountRow(account = account, onOpenAccount = onOpenAccount) } },
        )
    }
}

@Composable
private fun CreditAccountRow(
    account: Account,
    onOpenAccount: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    color = colors.tx,
                    fontSize = DhruvNextType.cardTitle,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(text = accountSubtitle(account), color = colors.tx2, fontSize = DhruvNextType.meta)
            }
            MoneyText(paise = account.balancePaise ?: 0L, color = colors.neg)
        }
        val limit = account.limitPaise
        if (limit != null && limit > 0) {
            val used = (-(account.balancePaise ?: 0L)).coerceAtLeast(0L)
            val utilisation = (used.toFloat() / limit.toFloat()).coerceIn(0f, 1f)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProgressRing(progress = utilisation, modifier = Modifier.size(20.dp), strokeWidth = 3.dp)
                Text(
                    text = "${(utilisation * 100).toInt()}% of limit " + Paise.format(limit, showDecimals = false),
                    color = colors.tx2,
                    fontSize = DhruvNextType.meta,
                )
                account.dueDay?.let { day ->
                    Text(text = "· due $day", color = colors.tx2, fontSize = DhruvNextType.meta)
                }
            }
        }
        TextButton(onClick = { onOpenAccount(account.id) }) {
            Text(text = "View details", color = colors.acc, fontSize = DhruvNextType.meta)
        }
    }
}

private fun accountSubtitle(account: Account): String {
    val typeLabel = accountTypeLabels[account.type.name].orEmpty()
    val maskLabel = account.mask?.let { "•••• $it" }
    val staleLabel = if (account.isStale()) account.staleMessage() else null
    return listOfNotNull(typeLabel, maskLabel, staleLabel).joinToString(" · ")
}
