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
import androidx.compose.ui.res.stringResource
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
                text =
                    stringResource(
                        if (uiState.correctingValuationId != null) R.string.c5_title_correct else R.string.c5_title_add,
                    ),
                color = colors.tx,
                fontWeight = FontWeight.Bold,
                fontSize = DhruvNextType.title,
            )

            uiState.lastValuePaise?.let { last ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text =
                            stringResource(
                                if (uiState.correctingValuationId != null) {
                                    R.string.c5_current_wrong_value
                                } else {
                                    R.string.c5_last_value
                                },
                            ),
                        color = colors.tx3,
                        fontSize = DhruvNextType.meta,
                    )
                    MoneyText(paise = last, variant = MoneyTextVariant.Inline)
                }
            }

            NxTextField(
                value = uiState.amountText,
                onValueChange = viewModel::onAmountChange,
                label = stringResource(R.string.c5_new_value_label),
                prefix = "₹",
                placeholder = "0",
                errorMessage = uiState.amountError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            viewModel.previewDelta(uiState)?.let { (deltaPaise, deltaBps) ->
                val deltaPercent = deltaBps / 100.0
                StatDeltaChip(
                    text =
                        stringResource(
                            R.string.c5_delta_format,
                            Paise.formatCompact(abs(deltaPaise)),
                            "%.1f".format(Locale.US, abs(deltaPercent)),
                        ),
                    isPositive = deltaPaise >= 0,
                )
            }

            if (uiState.correctingValuationId == null) {
                val sourceLabels =
                    listOf(
                        stringResource(R.string.c5_source_manual),
                        stringResource(R.string.c5_source_statement),
                        stringResource(R.string.c5_source_import),
                    )
                SegmentedRow(
                    options = sourceLabels,
                    selectedIndex = SELECTABLE_SOURCES.indexOfFirst { it.name == uiState.sourceCode }.coerceAtLeast(0),
                    onSelected = { index -> viewModel.onSourceChange(SELECTABLE_SOURCES[index].name) },
                )
            }

            Text(
                text = stringResource(R.string.c5_dated_today_note),
                color = colors.tx3,
                fontSize = DhruvNextType.meta,
            )

            NxButton(
                text = stringResource(R.string.networth_action_save),
                onClick = viewModel::save,
                loading = uiState.isSaving,
                block = true,
            )
        }
    }
}
