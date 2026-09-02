package com.dhruv.finance.networth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhruv.core.ui.components.NxButton
import com.dhruv.core.ui.components.NxSelect
import com.dhruv.core.ui.components.NxTextField
import com.dhruv.core.ui.components.NxTopBar
import com.dhruv.core.ui.components.SegmentedRow
import com.dhruv.core.ui.components.SelectionOption
import com.dhruv.core.ui.components.SelectionSheet
import com.dhruv.core.ui.components.SkeletonBlock
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import com.dhruv.finance.data.tracker.model.HoldingKind

/**
 * C4 — add or edit a holding. Add: "I own this"/"I owe this" toggle + sector picker (never free
 * text, NW-BR-004) + value entry. Edit (Phase 9, T051/T052): name/category/invested amount/notes
 * only — the current-value field is add-only (recording a new value is C5's job, not an edit), and
 * liability terms are not editable here (see [AddEditHoldingViewModel.startEditing]'s own doc).
 * [onClose] is this screen's only exit — the design law's "modal (close, not back)" presentation
 * class (`platform/DESIGN-SYSTEM.md` §6); rendered here via [NxTopBar]'s back-arrow slot since the
 * component has no separate close-icon variant yet (a design-system gap, not fixed in this phase).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHoldingScreen(
    viewModel: AddEditHoldingViewModel,
    onSaved: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var sheetOpen by remember { mutableStateOf(false) }
    var liabilityTypeSheetOpen by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.savedHoldingId) {
        uiState.savedHoldingId?.let(onSaved)
    }

    Column(modifier = modifier.fillMaxSize().background(colors.bg)) {
        NxTopBar(
            title = stringResource(if (uiState.isEditing) R.string.c4_title_edit else R.string.c4_title_add),
            onBack = onClose,
        )

        if (uiState.isLoadingForEdit) {
            Column(modifier = Modifier.padding(DhruvNextSpacing.screenGutter)) {
                SkeletonBlock(height = 160.dp)
            }
            return@Column
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(DhruvNextSpacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.inputGroupGap),
        ) {
            if (!uiState.isEditing) {
                SegmentedRow(
                    options = listOf(stringResource(R.string.c4_segment_own), stringResource(R.string.c4_segment_owe)),
                    selectedIndex = if (uiState.kind == HoldingKind.ASSET) 0 else 1,
                    onSelected = { index ->
                        viewModel.onKindChange(if (index == 0) HoldingKind.ASSET else HoldingKind.LIABILITY)
                    },
                )
            }
            NxTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = stringResource(R.string.c4_name_label),
                placeholder = stringResource(R.string.c4_name_placeholder),
                errorMessage = uiState.nameError,
            )
            NxSelect(
                value = uiState.sectorCode?.let { sectorLabel(it) },
                onClick = { sheetOpen = true },
                label = stringResource(R.string.c4_category_label),
                placeholder = stringResource(R.string.c4_category_placeholder),
                errorMessage = uiState.sectorError,
            )
            if (!uiState.isEditing) {
                NxTextField(
                    value = uiState.amountText,
                    onValueChange = viewModel::onAmountChange,
                    label = stringResource(R.string.c4_current_value_label),
                    prefix = "₹",
                    placeholder = "0",
                    errorMessage = uiState.amountError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Text(
                    text = stringResource(R.string.c4_value_history_disclaimer),
                    color = colors.tx3,
                    fontSize = DhruvNextType.meta,
                )
            }
            NxTextField(
                value = uiState.investedAmountText,
                onValueChange = viewModel::onInvestedAmountChange,
                label = stringResource(R.string.c4_invested_label),
                prefix = "₹",
                placeholder = stringResource(R.string.c4_invested_placeholder),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            NxTextField(
                value = uiState.notesText,
                onValueChange = viewModel::onNotesChange,
                label = stringResource(R.string.c4_notes_label),
                placeholder = stringResource(R.string.c4_notes_placeholder),
            )

            if (!uiState.isEditing && uiState.kind == HoldingKind.LIABILITY) {
                NxSelect(
                    value = uiState.liabilityTypeCode?.let { liabilityTypeLabel(it) },
                    onClick = { liabilityTypeSheetOpen = true },
                    label = stringResource(R.string.c4_liability_type_label),
                    placeholder = stringResource(R.string.c4_liability_type_placeholder),
                    errorMessage = uiState.liabilityTypeError,
                )
                NxTextField(
                    value = uiState.rateText,
                    onValueChange = viewModel::onRateChange,
                    label = stringResource(R.string.c4_rate_label),
                    placeholder = stringResource(R.string.c4_rate_placeholder),
                    suffix = stringResource(R.string.c4_rate_suffix),
                    errorMessage = uiState.rateError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                NxTextField(
                    value = uiState.emiText,
                    onValueChange = viewModel::onEmiChange,
                    label = stringResource(R.string.c4_emi_label),
                    prefix = "₹",
                    placeholder = "0",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                NxTextField(
                    value = uiState.tenureMonthsText,
                    onValueChange = viewModel::onTenureMonthsChange,
                    label = stringResource(R.string.c4_tenure_label),
                    placeholder = stringResource(R.string.c4_tenure_placeholder),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }

            uiState.liabilityMetaError?.let { message ->
                Text(text = message, color = colors.warn, fontSize = DhruvNextType.meta)
            }

            NxButton(
                text = stringResource(R.string.networth_action_save),
                onClick = viewModel::save,
                loading = uiState.isSaving,
                block = true,
            )
        }
    }

    if (sheetOpen) {
        SelectionSheet(
            title = stringResource(R.string.c4_choose_category_title),
            options = SectorLabels.map { (code, label) -> SelectionOption(id = code, label = label) },
            selectedId = uiState.sectorCode,
            onSelect = { option ->
                viewModel.onSectorChange(option.id)
                sheetOpen = false
            },
            onDismissRequest = { sheetOpen = false },
        )
    }

    if (liabilityTypeSheetOpen) {
        SelectionSheet(
            title = stringResource(R.string.c4_choose_liability_type_title),
            options = LiabilityTypeLabels.map { (code, label) -> SelectionOption(id = code, label = label) },
            selectedId = uiState.liabilityTypeCode,
            onSelect = { option ->
                viewModel.onLiabilityTypeChange(option.id)
                liabilityTypeSheetOpen = false
            },
            onDismissRequest = { liabilityTypeSheetOpen = false },
        )
    }
}
