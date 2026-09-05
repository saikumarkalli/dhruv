package com.dhruv.finance.money

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhruv.core.format.Paise
import com.dhruv.core.ui.components.ConfirmDangerDialog
import com.dhruv.core.ui.components.EmptyStateCard
import com.dhruv.core.ui.components.InfoBanner
import com.dhruv.core.ui.components.NxButton
import com.dhruv.core.ui.components.NxButtonVariant
import com.dhruv.core.ui.components.NxIconButton
import com.dhruv.core.ui.components.RetryErrorCard
import com.dhruv.core.ui.components.SkeletonBlock
import com.dhruv.core.ui.components.StatItem
import com.dhruv.core.ui.components.ThreeUpStatRow
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import com.dhruv.finance.data.tracker.model.RecurringTemplate
import com.dhruv.finance.data.tracker.repo.RecurringTemplateKeys
import java.time.format.DateTimeFormatter

/** D9 (recurring) — review banner, MONTHLY IN/OUT, NEXT 30 DAYS, PAUSED (spec.md Story 6). */
@Composable
fun RecurringScreen(
    viewModel: RecurringViewModel,
    onOpenReview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalDhruvNextColors.current
    var deleteTarget by remember { mutableStateOf<RecurringTemplate?>(null) }

    when (val current = state) {
        is RecurringUiState.Loading -> SkeletonBlock(modifier = modifier.fillMaxSize())
        is RecurringUiState.Error -> RetryErrorCard(message = current.message, onRetry = { viewModel.load() })
        is RecurringUiState.Loaded ->
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(DhruvNextSpacing.screenGutter),
            ) {
                if (current.pendingCount > 0) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = DhruvNextSpacing.interCardGap),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            InfoBanner(
                                message = "${current.pendingCount} entries need review",
                                modifier = Modifier.weight(1f),
                            )
                            NxButton(text = "Review", onClick = onOpenReview, variant = NxButtonVariant.Soft)
                        }
                    }
                }
                item {
                    ThreeUpStatRow(
                        modifier = Modifier.padding(bottom = DhruvNextSpacing.sectionGap),
                        items =
                            listOf(
                                StatItem("MONTHLY IN", Paise.format(current.monthlyInPaise, showDecimals = false)),
                                StatItem("MONTHLY OUT", Paise.format(current.monthlyOutPaise, showDecimals = false)),
                            ),
                    )
                }
                if (current.next30Days.isEmpty() && current.paused.isEmpty()) {
                    item { EmptyStateCard(message = "No recurring entries yet") }
                } else {
                    item {
                        SectionLabel("NEXT 30 DAYS", colors, Modifier.padding(bottom = 8.dp))
                    }
                    items(current.next30Days, key = { it.id }) { template ->
                        RecurringRow(
                            template,
                            onPause = { viewModel.pause(template.id) },
                            onDelete = { deleteTarget = template },
                        )
                    }
                    if (current.paused.isNotEmpty()) {
                        item {
                            SectionLabel(
                                "PAUSED",
                                colors,
                                Modifier.padding(top = DhruvNextSpacing.sectionGap, bottom = 8.dp),
                            )
                        }
                        items(current.paused, key = { it.id }) { template ->
                            RecurringRow(
                                template,
                                isPaused = true,
                                onResume = { viewModel.resume(template.id) },
                                onDelete = { deleteTarget = template },
                            )
                        }
                    }
                }
            }
    }

    deleteTarget?.let { template ->
        val payee = template.template[RecurringTemplateKeys.PAYEE] as? String ?: "this recurring entry"
        ConfirmDangerDialog(
            title = "Delete \"$payee\"?",
            body = "This stops future occurrences and withdraws any of its entries still waiting for review. This cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = {
                viewModel.delete(template.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun SectionLabel(
    text: String,
    colors: com.dhruv.core.ui.theme.DhruvNextColors,
    modifier: Modifier,
) {
    Text(
        text = text,
        color = colors.tx2,
        fontSize = DhruvNextType.sectionLabel,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier,
    )
}

@Composable
private fun RecurringRow(
    template: RecurringTemplate,
    isPaused: Boolean = false,
    onResume: (() -> Unit)? = null,
    onPause: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val colors = LocalDhruvNextColors.current
    var menuExpanded by remember { mutableStateOf(false) }
    val amountPaise = (template.template[RecurringTemplateKeys.AMOUNT_PAISE] as? Number)?.toLong() ?: 0L
    val payee = template.template[RecurringTemplateKeys.PAYEE] as? String ?: "Recurring"
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(payee, color = colors.tx, fontSize = DhruvNextType.cardTitle)
            val subtitle =
                if (isPaused) {
                    "Paused"
                } else {
                    val label = if (template.amountIsVariable) "Variable" else "Auto-debit"
                    "$label · ${template.nextRun.format(DateTimeFormatter.ofPattern("d MMM"))}"
                }
            Text(subtitle, color = colors.tx2, fontSize = DhruvNextType.meta)
        }
        Text(
            Paise.format(amountPaise),
            color = colors.tx,
            fontSize = DhruvNextType.cardTitle,
            fontWeight = FontWeight.Bold,
        )
        if (isPaused && onResume != null) {
            NxButton(text = "Resume", onClick = onResume, variant = NxButtonVariant.Soft)
        }
        Box {
            NxIconButton(
                icon = Icons.Default.MoreVert,
                onClick = { menuExpanded = true },
                contentDescription = "More actions for $payee",
            )
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                if (!isPaused && onPause != null) {
                    DropdownMenuItem(text = { Text("Pause") }, onClick = { menuExpanded = false; onPause() })
                }
                if (onDelete != null) {
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menuExpanded = false; onDelete() })
                }
            }
        }
    }
}
