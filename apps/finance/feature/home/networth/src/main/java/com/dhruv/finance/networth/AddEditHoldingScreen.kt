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
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhruv.core.ui.components.NxButton
import com.dhruv.core.ui.components.NxSelect
import com.dhruv.core.ui.components.NxTextField
import com.dhruv.core.ui.components.NxTopBar
import com.dhruv.core.ui.components.SegmentedRow
import com.dhruv.core.ui.components.SelectionOption
import com.dhruv.core.ui.components.SelectionSheet
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import com.dhruv.finance.data.tracker.model.HoldingKind

/**
 * C4 — add a holding, "I own this"/"I owe this" toggle + sector picker (never free text, NW-BR-004)
 * + value entry. [onClose] is this screen's only exit — the design law's "modal (close, not back)"
 * presentation class (`platform/DESIGN-SYSTEM.md` §6); rendered here via [NxTopBar]'s back-arrow
 * slot since the component has no separate close-icon variant yet (a design-system gap, not fixed
 * in this phase).
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
        NxTopBar(title = "Add holding", onBack = onClose)

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(DhruvNextSpacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.inputGroupGap),
        ) {
            SegmentedRow(
                options = listOf("I own this", "I owe this"),
                selectedIndex = if (uiState.kind == HoldingKind.ASSET) 0 else 1,
                onSelected = { index ->
                    viewModel.onKindChange(if (index == 0) HoldingKind.ASSET else HoldingKind.LIABILITY)
                },
            )
            NxTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = "Name",
                placeholder = "e.g. HDFC Savings",
                errorMessage = uiState.nameError,
            )
            NxSelect(
                value = uiState.sectorCode?.let { sectorLabel(it) },
                onClick = { sheetOpen = true },
                label = "Category",
                placeholder = "Choose a category",
                errorMessage = uiState.sectorError,
            )
            NxTextField(
                value = uiState.amountText,
                onValueChange = viewModel::onAmountChange,
                label = "Current value",
                prefix = "₹",
                placeholder = "0",
                errorMessage = uiState.amountError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Text(
                text =
                    "Every value you save stays in the app's history — you can add a corrected " +
                        "value later, but this one is never edited or removed.",
                color = colors.tx3,
                fontSize = DhruvNextType.meta,
            )

            if (uiState.kind == HoldingKind.LIABILITY) {
                NxSelect(
                    value = uiState.liabilityTypeCode?.let { liabilityTypeLabel(it) },
                    onClick = { liabilityTypeSheetOpen = true },
                    label = "Liability type",
                    placeholder = "Choose a type",
                    errorMessage = uiState.liabilityTypeError,
                )
                NxTextField(
                    value = uiState.rateText,
                    onValueChange = viewModel::onRateChange,
                    label = "Interest rate",
                    placeholder = "e.g. 8.5",
                    suffix = "% p.a.",
                    errorMessage = uiState.rateError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                NxTextField(
                    value = uiState.emiText,
                    onValueChange = viewModel::onEmiChange,
                    label = "Monthly payment (optional)",
                    prefix = "₹",
                    placeholder = "0",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                NxTextField(
                    value = uiState.tenureMonthsText,
                    onValueChange = viewModel::onTenureMonthsChange,
                    label = "Tenure in months (optional)",
                    placeholder = "e.g. 240",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }

            uiState.liabilityMetaError?.let { message ->
                Text(text = message, color = colors.warn, fontSize = DhruvNextType.meta)
            }

            NxButton(
                text = "Save",
                onClick = viewModel::save,
                loading = uiState.isSaving,
                block = true,
            )
        }
    }

    if (sheetOpen) {
        SelectionSheet(
            title = "Choose a category",
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
            title = "Choose a liability type",
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
