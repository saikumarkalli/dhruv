package com.dhruv.finance.networth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhruv.core.format.Paise
import com.dhruv.core.ui.components.DhruvModalSheet
import com.dhruv.core.ui.components.MoneyText
import com.dhruv.core.ui.components.MoneyTextVariant
import com.dhruv.core.ui.components.NxButton
import com.dhruv.core.ui.components.NxTextField
import com.dhruv.core.ui.components.SegmentedRow
import com.dhruv.core.ui.components.StatDeltaChip
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import com.dhruv.finance.data.tracker.model.ValuationSource
import java.util.Locale
import kotlin.math.abs

private val SELECTABLE_SOURCES = listOf(ValuationSource.MANUAL, ValuationSource.STATEMENT, ValuationSource.IMPORT)
private val SOURCE_LABELS = listOf("Manual", "Statement", "Import")

/**
 * C5 — add a value, or (when [AddValuationViewModel.UiState.correctingValuationId] is set)
 * correct one specific existing entry (spec.md Story 3, NW-UI-003). The source picker is hidden
 * in correction mode — `finance.correct_valuation` always writes `source = 'CORRECTION'`
 * server-side; there is nothing for the user to choose.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddValuationSheet(
    viewModel: AddValuationViewModel,
    onSaved: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.savedValuationId) {
        if (uiState.savedValuationId != null) onSaved()
    }

    DhruvModalSheet(onDismissRequest = onDismissRequest, modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(DhruvNextSpacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.inputGroupGap),
        ) {
            Text(
                text = if (uiState.correctingValuationId != null) "Correct this value" else "Add a value",
                color = colors.tx,
                fontWeight = FontWeight.Bold,
                fontSize = DhruvNextType.title,
            )

            uiState.lastValuePaise?.let { last ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = if (uiState.correctingValuationId != null) "Current (wrong) value" else "Last value",
                        color = colors.tx3,
                        fontSize = DhruvNextType.meta,
                    )
                    MoneyText(paise = last, variant = MoneyTextVariant.Inline)
                }
            }

            NxTextField(
                value = uiState.amountText,
                onValueChange = viewModel::onAmountChange,
                label = "New value",
                prefix = "₹",
                placeholder = "0",
                errorMessage = uiState.amountError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            viewModel.previewDelta(uiState)?.let { (deltaPaise, deltaBps) ->
                val deltaPercent = deltaBps / 100.0
                StatDeltaChip(
                    text = "${Paise.formatCompact(abs(deltaPaise))} (%.1f%%)".format(Locale.US, abs(deltaPercent)),
                    isPositive = deltaPaise >= 0,
                )
            }

            if (uiState.correctingValuationId == null) {
                SegmentedRow(
                    options = SOURCE_LABELS,
                    selectedIndex = SELECTABLE_SOURCES.indexOfFirst { it.name == uiState.sourceCode }.coerceAtLeast(0),
                    onSelected = { index -> viewModel.onSourceChange(SELECTABLE_SOURCES[index].name) },
                )
            }

            Text(
                text = "Dated today — back-dating a value isn't supported yet.",
                color = colors.tx3,
                fontSize = DhruvNextType.meta,
            )

            NxButton(
                text = "Save",
                onClick = viewModel::save,
                loading = uiState.isSaving,
                block = true,
            )
        }
    }
}
