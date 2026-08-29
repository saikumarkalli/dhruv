package com.dhruv.finance.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dhruv.core.navigation.NavTarget
import com.dhruv.core.ui.components.ConfirmDangerDialog
import com.dhruv.core.ui.components.ListGroup
import com.dhruv.core.ui.components.ListGroupRow
import com.dhruv.core.ui.components.NxButton
import com.dhruv.core.ui.components.NxButtonVariant
import com.dhruv.core.ui.components.NxTextField
import com.dhruv.core.ui.components.SectionLabel
import com.dhruv.core.ui.components.SegmentedRow
import com.dhruv.core.ui.components.Stepper
import com.dhruv.core.ui.components.SwitchRow
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import com.dhruv.finance.app.R
import com.dhruv.settings.contribution.SettingsRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * The "optionally-labeled group of rows" shape every settings surface repeats — `ModuleSettingsScreen`,
 * `AppSettingsScreen`'s Appearance/Security/Notifications sections (Article VI: extend, never
 * duplicate rendering logic, T092). `null` [label] renders an ungrouped [ListGroup] at the top of
 * its surface, matching contract §1's "null = ungrouped rows".
 */
@Composable
fun LabeledSettingsGroup(
    label: Int?,
    modifier: Modifier = Modifier,
    rows: List<@Composable () -> Unit>,
) {
    if (label != null) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(text = stringResource(label), modifier = Modifier.padding(start = 4.dp))
            ListGroup(rows = rows)
        }
    } else {
        ListGroup(rows = rows, modifier = modifier)
    }
}

/**
 * The single place a [SettingsRow] becomes a component (contract §2) — every module's row renders
 * through here, built only from existing `:libs:core` components (constitution VI). No module ever
 * draws its own row.
 *
 * [onNavigate] handles [SettingsRow.Navigate] — the renderer doesn't know how to navigate itself,
 * only that the shell does. [onError] receives a row's Flow throwing while collecting (contract §4
 * rule 12, `SET-ARCH-007`) instead of crashing — [ModuleSettingsScreen] turns it into that one
 * entry's `FeatureErrorCard`.
 */
@Composable
fun SettingsRowRenderer(
    row: SettingsRow,
    modifier: Modifier = Modifier,
    onNavigate: (NavTarget) -> Unit = {},
    onError: (Throwable) -> Unit = {},
) {
    when (row) {
        is SettingsRow.Toggle -> ToggleRow(row, modifier, onError)
        is SettingsRow.Choice -> ChoiceRow(row, modifier, onError)
        is SettingsRow.Stepper -> StepperRow(row, modifier, onError)
        is SettingsRow.Action -> ActionRow(row, modifier)
        is SettingsRow.Navigate -> NavigateRow(row, modifier, onNavigate)
        is SettingsRow.Info -> InfoRow(row, modifier, onError)
        is SettingsRow.SecretText -> SecretTextRow(row, modifier, onError)
    }
}

@Composable
private fun ToggleRow(
    row: SettingsRow.Toggle,
    modifier: Modifier,
    onError: (Throwable) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var persisted by remember(row.key) { mutableStateOf(false) }
    var displayed by remember(row.key) { mutableStateOf(false) }
    var errorMessage by remember(row.key) { mutableStateOf<String?>(null) }
    val writeFailedMessage = stringResource(R.string.settings_row_write_failed)

    // FR-042/contract rule 9: the row reflects the persisted flow directly until a local write is
    // in flight, so a value changed elsewhere (another surface, another device write) still shows.
    LaunchedEffectCollect(row.value, onError) { value ->
        persisted = value
        displayed = value
    }

    SwitchRow(
        label = stringResource(row.label),
        description = errorMessage ?: stringResource(row.description),
        checked = displayed,
        onCheckedChange = { newValue ->
            if (!row.enabled) return@SwitchRow
            displayed = newValue
            errorMessage = null
            scope.launch {
                runCatching { row.onChange(newValue) }
                    .onFailure {
                        displayed = persisted
                        errorMessage = writeFailedMessage
                    }
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun ChoiceRow(
    row: SettingsRow.Choice,
    modifier: Modifier,
    onError: (Throwable) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var selectedId by remember(row.key) { mutableStateOf("") }
    var errorMessage by remember(row.key) { mutableStateOf<String?>(null) }
    val writeFailedMessage = stringResource(R.string.settings_row_write_failed)

    LaunchedEffectCollect(row.selected, onError) { selectedId = it }

    fun select(id: String) {
        if (!row.enabled) return
        val previous = selectedId
        selectedId = id
        errorMessage = null
        scope.launch {
            runCatching { row.onSelect(id) }
                .onFailure {
                    selectedId = previous
                    errorMessage = writeFailedMessage
                }
        }
    }

    val colors = LocalDhruvNextColors.current
    if (row.options.size <= 3) {
        Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(text = stringResource(row.label), color = colors.tx, fontSize = DhruvNextType.cardTitle)
            errorMessage?.let { Text(text = it, color = colors.neg, fontSize = DhruvNextType.meta) }
            SegmentedRow(
                options = row.options.map { stringResource(it.label) },
                selectedIndex = row.options.indexOfFirst { it.id == selectedId }.coerceAtLeast(0),
                onSelected = { index -> select(row.options[index].id) },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
        }
    } else {
        // No `SelectionSheet` exists yet (design system §5.2, batch B9 — planned, not built).
        // Article VI forbids inventing a parallel component here, so >3 options render as a plain
        // vertical list of existing ListGroupRow entries with a checkmark on the selected one.
        Column(modifier = modifier.fillMaxWidth()) {
            row.options.forEach { option ->
                ListGroupRow(
                    title = stringResource(option.label),
                    onClick = { select(option.id) },
                    showChevron = false,
                    trailing = {
                        if (option.id == selectedId) {
                            Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = colors.acc)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun StepperRow(
    row: SettingsRow.Stepper,
    modifier: Modifier,
    onError: (Throwable) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var value by remember(row.key) { mutableStateOf(row.range.first) }
    var errorMessage by remember(row.key) { mutableStateOf<String?>(null) }
    val writeFailedMessage = stringResource(R.string.settings_row_write_failed)

    LaunchedEffectCollect(row.value, onError) { value = it }

    val colors = LocalDhruvNextColors.current
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(row.label), color = colors.tx, fontSize = DhruvNextType.cardTitle)
            Text(text = errorMessage ?: stringResource(row.description), color = colors.tx2, fontSize = DhruvNextType.meta)
        }
        Stepper(
            value = value,
            min = row.range.first,
            max = row.range.last,
            onValueChange = { newValue ->
                if (!row.enabled) return@Stepper
                val previous = value
                value = newValue
                errorMessage = null
                scope.launch {
                    runCatching { row.onChange(newValue) }
                        .onFailure {
                            value = previous
                            errorMessage = writeFailedMessage
                        }
                }
            },
        )
    }
}

@Composable
private fun ActionRow(
    row: SettingsRow.Action,
    modifier: Modifier,
) {
    val scope = rememberCoroutineScope()
    var showConfirm by remember(row.key) { mutableStateOf(false) }
    var errorMessage by remember(row.key) { mutableStateOf<String?>(null) }
    val actionFailedMessage = stringResource(R.string.settings_row_action_failed)

    fun invokeAction() {
        scope.launch {
            row.onInvoke().onFailure { errorMessage = actionFailedMessage }
        }
    }

    ListGroupRow(
        title = stringResource(row.label),
        subtitle = errorMessage ?: stringResource(row.description),
        showChevron = false,
        onClick =
            if (row.enabled) {
                { if (row.confirm != null) showConfirm = true else invokeAction() }
            } else {
                null
            },
        modifier = modifier,
    )

    val confirm = row.confirm
    if (showConfirm && confirm != null) {
        ConfirmDangerDialog(
            title = stringResource(confirm.title),
            body = stringResource(confirm.body),
            confirmLabel = stringResource(confirm.confirmLabel),
            onConfirm = {
                showConfirm = false
                invokeAction()
            },
            onDismiss = { showConfirm = false },
        )
    }
}

@Composable
private fun NavigateRow(
    row: SettingsRow.Navigate,
    modifier: Modifier,
    onNavigate: (NavTarget) -> Unit,
) {
    ListGroupRow(
        title = stringResource(row.label),
        subtitle = stringResource(row.description),
        onClick = if (row.enabled) ({ onNavigate(row.target) }) else null,
        modifier = modifier,
    )
}

/**
 * FR-038/`SET-BR-012`/T097: a stored secret shows masked with a single-action remove, never its
 * real value. Unset shows an editable field with an explicit save — the one row type FR-042's
 * "persist immediately" does not apply to (see [SettingsRow.SecretText]'s own doc).
 */
@Composable
private fun SecretTextRow(
    row: SettingsRow.SecretText,
    modifier: Modifier,
    onError: (Throwable) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var stored by remember(row.key) { mutableStateOf<String?>(null) }
    var draft by remember(row.key) { mutableStateOf("") }
    var errorMessage by remember(row.key) { mutableStateOf<String?>(null) }
    val writeFailedMessage = stringResource(R.string.settings_row_write_failed)

    LaunchedEffectCollect(row.value, onError) { stored = it }

    val colors = LocalDhruvNextColors.current
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(text = stringResource(row.label), color = colors.tx, fontSize = DhruvNextType.cardTitle)
        Text(
            text = errorMessage ?: stringResource(row.description),
            color = if (errorMessage != null) colors.neg else colors.tx2,
            fontSize = DhruvNextType.meta,
        )

        val currentValue = stored
        if (currentValue != null) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.settings_secret_masked),
                    color = colors.tx,
                    fontSize = DhruvNextType.cardTitle,
                    modifier = Modifier.weight(1f),
                )
                NxButton(
                    text = stringResource(R.string.settings_secret_remove),
                    variant = NxButtonVariant.Destructive,
                    onClick = { scope.launch { runCatching { row.onRemove() }.onFailure { errorMessage = writeFailedMessage } } },
                )
            }
        } else {
            NxTextField(
                value = draft,
                onValueChange = { draft = it; errorMessage = null },
                placeholder = stringResource(R.string.settings_secret_placeholder),
                errorMessage = errorMessage,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            NxButton(
                text = stringResource(R.string.settings_secret_save),
                onClick = {
                    val toSave = draft
                    if (toSave.isBlank()) return@NxButton
                    scope.launch {
                        runCatching { row.onSave(toSave) }
                            .onSuccess { draft = "" }
                            .onFailure { errorMessage = writeFailedMessage }
                    }
                },
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun InfoRow(
    row: SettingsRow.Info,
    modifier: Modifier,
    onError: (Throwable) -> Unit,
) {
    var text by remember(row.key) { mutableStateOf("") }
    LaunchedEffectCollect(row.value, onError) { text = it }
    val colors = LocalDhruvNextColors.current
    ListGroupRow(
        title = stringResource(row.label),
        showChevron = false,
        modifier = modifier,
        trailing = { Text(text = text, color = colors.tx2, fontSize = DhruvNextType.body) },
    )
}

/**
 * Small local wrapper so every row above collects its Flow the same way, once per composition.
 * A throwing flow reports through [onError] (`SET-ARCH-007`) rather than crashing the coroutine —
 * [ModuleSettingsScreen] turns it into that entry's own error card.
 */
@Composable
private fun <T> LaunchedEffectCollect(
    flow: Flow<T>,
    onError: (Throwable) -> Unit,
    onEach: suspend (T) -> Unit,
) {
    LaunchedEffect(flow) {
        flow.catch { onError(it) }.collectLatest(onEach)
    }
}
