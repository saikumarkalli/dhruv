package com.dhruv.finance.money

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhruv.core.format.Paise
import com.dhruv.core.ui.components.ConfirmDangerDialog
import com.dhruv.core.ui.components.DhruvModalSheet
import com.dhruv.core.ui.components.ListGroup
import com.dhruv.core.ui.components.ListGroupRow
import com.dhruv.core.ui.components.MoneyText
import com.dhruv.core.ui.components.MoneyTextVariant
import com.dhruv.core.ui.components.NxButton
import com.dhruv.core.ui.components.NxButtonVariant
import com.dhruv.core.ui.components.NxTextField
import com.dhruv.core.ui.components.OfflineStateCard
import com.dhruv.core.ui.components.ReconcileBanner
import com.dhruv.core.ui.components.RetryErrorCard
import com.dhruv.core.ui.components.SectionLabel
import com.dhruv.core.ui.components.SignedOutCard
import com.dhruv.core.ui.components.SkeletonBlock
import com.dhruv.core.ui.components.StatItem
import com.dhruv.core.ui.components.ThreeUpStatRow
import com.dhruv.core.ui.components.TrendSparkline
import com.dhruv.core.ui.theme.DhruvBrand
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import com.dhruv.finance.data.tracker.model.TransactionType
import com.dhruv.finance.money.MoneyConfig.accountTypeLabels
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * D7 (account detail, US3) — balance, masked number, primary badge, balance trend, month in/out,
 * recent activity with a running balance, and [ReconcileBanner] when stale (FR-019/020/021).
 * Full screen-state set per FR-032 (T047).
 */
@Composable
fun AccountDetailScreen(
    viewModel: AccountDetailViewModel,
    onAddTransaction: () -> Unit,
    onSignIn: () -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deletePrompt by viewModel.deletePrompt.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    var showReconcileSheet by remember { mutableStateOf(false) }

    LaunchedEffect(deleted) { if (deleted) onDeleted() }

    Box(modifier = modifier.fillMaxSize()) {
        when (val current = state) {
            is AccountDetailUiState.Loading -> SkeletonBlock(modifier = Modifier.fillMaxSize())
            is AccountDetailUiState.Error ->
                RetryErrorCard(
                    message = current.message,
                    onRetry = { viewModel.load() },
                    modifier = Modifier.padding(DhruvNextSpacing.screenGutter),
                )
            is AccountDetailUiState.Offline ->
                OfflineStateCard(onRetry = { viewModel.load() }, modifier = Modifier.padding(DhruvNextSpacing.screenGutter))
            is AccountDetailUiState.SignedOut ->
                SignedOutCard(
                    message = "Sign in to see this account.",
                    actionLabel = "Sign in",
                    onAction = onSignIn,
                    modifier = Modifier.padding(DhruvNextSpacing.screenGutter),
                )
            is AccountDetailUiState.Loaded ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(DhruvNextSpacing.screenGutter),
                ) {
                    BalanceHeader(current = current)

                    if (current.isStale) {
                        ReconcileBanner(
                            message = current.staleMessage,
                            onReconcile = { showReconcileSheet = true },
                            modifier = Modifier.padding(top = DhruvNextSpacing.interCardGap),
                        )
                    }

                    if (current.trend.size >= 2) {
                        SectionLabel(text = "Balance trend", modifier = Modifier.padding(top = DhruvNextSpacing.sectionGap, bottom = 8.dp))
                        TrendSparkline(
                            values = current.trend,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .semantics {
                                        contentDescription =
                                            "Balance trend, from ${Paise.format(current.trend.first().toLong())} " +
                                                "to ${Paise.format(current.trend.last().toLong())}"
                                    },
                        )
                    }

                    SectionLabel(text = "This month", modifier = Modifier.padding(top = DhruvNextSpacing.sectionGap, bottom = 8.dp))
                    ThreeUpStatRow(
                        items =
                            listOf(
                                StatItem("IN", Paise.format(current.monthInPaise, showDecimals = false)),
                                StatItem("OUT", Paise.format(current.monthOutPaise, showDecimals = false)),
                                StatItem(
                                    "NET",
                                    Paise.format(current.monthInPaise - current.monthOutPaise, showDecimals = false),
                                    highlighted = true,
                                ),
                            ),
                    )

                    SectionLabel(text = "Recent activity", modifier = Modifier.padding(top = DhruvNextSpacing.sectionGap, bottom = 8.dp))
                    if (current.activity.isEmpty()) {
                        Text(
                            text = "Nothing recorded on this account yet.",
                            color = LocalDhruvNextColors.current.tx2,
                            fontSize = DhruvNextType.body,
                        )
                    } else {
                        ListGroup(
                            rows =
                                current.activity.map { row ->
                                    {
                                        ListGroupRow(
                                            title = row.transaction.payee ?: row.transaction.type.name,
                                            subtitle =
                                                "Balance " +
                                                    Paise.format(row.runningBalancePaise, showDecimals = false),
                                            showChevron = false,
                                            trailing = {
                                                MoneyText(
                                                    paise = row.transaction.amountPaise,
                                                    color =
                                                        if (row.transaction.type == TransactionType.INCOME) {
                                                            LocalDhruvNextColors.current.pos
                                                        } else {
                                                            LocalDhruvNextColors.current.neg
                                                        },
                                                )
                                            },
                                        )
                                    }
                                },
                        )
                    }

                    NxButton(
                        text = "Add transaction",
                        onClick = onAddTransaction,
                        block = true,
                        modifier = Modifier.padding(top = DhruvNextSpacing.sectionGap),
                    )

                    NxButton(
                        text = "Delete account",
                        onClick = { viewModel.requestDelete() },
                        variant = NxButtonVariant.Destructive,
                        block = true,
                        modifier = Modifier.padding(top = DhruvNextSpacing.interCardGap),
                    )
                }
        }
    }

    val loaded = state as? AccountDetailUiState.Loaded
    if (showReconcileSheet && loaded != null) {
        ReconcileSheet(
            currentBalancePaise = loaded.account.balancePaise ?: loaded.account.openingBalancePaise,
            onDismissRequest = { showReconcileSheet = false },
            onConfirm = { statedBalancePaise ->
                viewModel.reconcile(statedBalancePaise)
                showReconcileSheet = false
            },
        )
    }

    val prompt = deletePrompt
    if (prompt is AccountDeletePrompt.Confirm) {
        ConfirmDangerDialog(
            title = "Delete this account?",
            body =
                if (prompt.transactionCount == 0) {
                    "This account has no transactions. This cannot be undone."
                } else {
                    "It has ${prompt.transactionCount} transaction" +
                        (if (prompt.transactionCount == 1) "" else "s") +
                        ". They are not deleted — they stay recorded, this account just no longer appears in your accounts list. This cannot be undone."
                },
            confirmLabel = "Delete",
            onConfirm = { viewModel.confirmDelete() },
            onDismiss = { viewModel.dismissDeletePrompt() },
        )
    }
}

/** D7's own dark-hero surface (DESIGN-SYSTEM §1: brand chrome is theme-invariant) — the functional
 * spec names D7 a dark-hero screen; this reads [DhruvBrand] rather than [LocalDhruvNextColors] so
 * it does not flip with the system theme, same as the design's own hero gradient cards. */
@Composable
private fun BalanceHeader(current: AccountDetailUiState.Loaded) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DhruvNextRadii.card))
                .background(
                    Brush.verticalGradient(listOf(DhruvBrand.navyElevated, DhruvBrand.navy)),
                ).padding(DhruvNextSpacing.cardPadding),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(text = current.account.name, color = DhruvBrand.silverLight, fontSize = DhruvNextType.title, fontWeight = FontWeight.Bold)
                val typeLabel = accountTypeLabels[current.account.type.name].orEmpty()
                val maskLabel = current.account.mask?.let { "•••• $it" }
                Text(
                    text = listOfNotNull(typeLabel, maskLabel).joinToString(" · "),
                    color = DhruvBrand.steel,
                    fontSize = DhruvNextType.meta,
                )
            }
            if (current.account.isPrimary) {
                Text(
                    text = "PRIMARY",
                    color = DhruvBrand.accentBlue,
                    fontSize = DhruvNextType.sectionLabel,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        MoneyText(
            paise = current.account.balancePaise ?: current.account.openingBalancePaise,
            variant = MoneyTextVariant.Hero,
            color = DhruvBrand.silverLight,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** A minimal amount-entry sheet for FR-021's "enter the real current balance" step — deliberately
 * lightweight (a single [NxTextField]) since reconciliation is an infrequent, deliberate action,
 * unlike D2's three-tap quick-add keypad. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ReconcileSheet(
    currentBalancePaise: Long,
    onDismissRequest: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    var text by remember { mutableStateOf((currentBalancePaise / 100.0).toString()) }
    val colors = LocalDhruvNextColors.current

    DhruvModalSheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.fillMaxWidth().padding(DhruvNextSpacing.screenGutter)) {
            Text(text = "Reconcile balance", color = colors.tx, fontSize = DhruvNextType.title, fontWeight = FontWeight.Bold)
            Text(
                text = "Enter the real current balance shown by your bank or app.",
                color = colors.tx2,
                fontSize = DhruvNextType.meta,
                modifier = Modifier.padding(top = 4.dp, bottom = DhruvNextSpacing.interCardGap),
            )
            NxTextField(
                value = text,
                onValueChange = { text = it },
                label = "Real balance",
                prefix = "₹",
                modifier = Modifier.fillMaxWidth(),
            )
            NxButton(
                text = "Reconcile",
                onClick = {
                    val rupees = text.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val paise = rupees.setScale(2, RoundingMode.HALF_UP).movePointRight(2).toLong()
                    onConfirm(paise)
                },
                block = true,
                modifier = Modifier.padding(top = DhruvNextSpacing.sectionGap),
            )
        }
    }
}

private fun String.toBigDecimalOrNull(): BigDecimal? = runCatching { BigDecimal(this) }.getOrNull()
