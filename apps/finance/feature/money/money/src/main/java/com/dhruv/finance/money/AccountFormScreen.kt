package com.dhruv.finance.money

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhruv.core.ui.components.NxButton
import com.dhruv.core.ui.components.NxTextField
import com.dhruv.core.ui.components.SegmentedRow
import com.dhruv.core.ui.components.SwitchRow
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import com.dhruv.finance.data.tracker.model.Account
import com.dhruv.finance.data.tracker.model.AccountType
import com.dhruv.finance.money.MoneyConfig.accountTypeLabels
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * D6's add/edit account form (US3, FR-016). [existing] non-null opens in edit mode; null opens a
 * fresh create form. Full-screen modal (close ✕, matching D3's own convention — no `NxTopBar`
 * close variant yet, see `TransactionFormScreen`'s note).
 */
@Composable
fun AccountFormScreen(
    viewModel: AccountFormViewModel,
    existing: Account?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalDhruvNextColors.current

    LaunchedEffect(existing?.id) { viewModel.open(existing) }
    LaunchedEffect(state.savedAccountId) { if (state.savedAccountId != null) onClose() }

    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = colors.tx)
            }
            Text(
                text = if (state.isEditing) "Edit account" else "New account",
                color = colors.tx,
                fontSize = DhruvNextType.title,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(DhruvNextSpacing.screenGutter),
        ) {
            NxTextField(
                value = state.name,
                onValueChange = { viewModel.setName(it) },
                label = "Name",
                modifier = Modifier.fillMaxWidth(),
            )

            SegmentedRow(
                options = AccountType.entries.map { accountTypeLabels.getValue(it.name) },
                selectedIndex = AccountType.entries.indexOf(state.type),
                onSelected = { index -> viewModel.setType(AccountType.entries[index]) },
                modifier = Modifier.padding(top = DhruvNextSpacing.interCardGap),
            )

            NxTextField(
                value = state.mask,
                onValueChange = { viewModel.setMask(it) },
                label = "Last 4 digits (optional)",
                placeholder = "1234",
                modifier = Modifier.fillMaxWidth().padding(top = DhruvNextSpacing.interCardGap),
            )

            NxTextField(
                value = if (state.openingBalancePaise == 0L) "" else (state.openingBalancePaise / 100.0).toString(),
                onValueChange = { text -> viewModel.setOpeningBalance(text.toRupeesPaiseOrZero()) },
                label = "Opening balance",
                prefix = "₹",
                modifier = Modifier.fillMaxWidth().padding(top = DhruvNextSpacing.interCardGap),
            )

            if (state.type == AccountType.CREDIT_CARD) {
                NxTextField(
                    value = state.limitPaise?.let { (it / 100.0).toString() }.orEmpty(),
                    onValueChange = { text -> viewModel.setLimit(text.toRupeesPaiseOrNull()) },
                    label = "Credit limit",
                    prefix = "₹",
                    modifier = Modifier.fillMaxWidth().padding(top = DhruvNextSpacing.interCardGap),
                )
                NxTextField(
                    value = state.dueDay?.toString().orEmpty(),
                    onValueChange = { text -> viewModel.setDueDay(text.toIntOrNull()) },
                    label = "Payment due day (1-31)",
                    modifier = Modifier.fillMaxWidth().padding(top = DhruvNextSpacing.interCardGap),
                )
            }

            SwitchRow(
                label = "Primary account",
                checked = state.isPrimary,
                onCheckedChange = { viewModel.setPrimary(it) },
                modifier = Modifier.padding(top = DhruvNextSpacing.interCardGap),
            )

            if (state.validationError != null) {
                Text(
                    text = state.validationError.orEmpty(),
                    color = colors.neg,
                    fontSize = DhruvNextType.meta,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            NxButton(
                text = "Save",
                onClick = { viewModel.save() },
                loading = state.isSaving,
                block = true,
                modifier = Modifier.padding(top = DhruvNextSpacing.sectionGap),
            )
        }
    }
}

private fun String.toRupeesPaiseOrZero(): Long = toRupeesPaiseOrNull() ?: 0L

private fun String.toRupeesPaiseOrNull(): Long? =
    toDoubleOrNull()?.let { rupees ->
        BigDecimal(rupees.toString()).setScale(2, RoundingMode.HALF_UP).movePointRight(2).toLong()
    }
