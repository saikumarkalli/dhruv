package com.dhruv.finance.app.ui.plan

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dhruv.core.navigation.PlanTool
import com.dhruv.core.ui.components.ListGroup
import com.dhruv.core.ui.components.ListGroupRow
import com.dhruv.core.ui.components.SectionLabel
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvTheme

/**
 * Plan tab launcher — DhruvNext §6.4
 * (`docs/superpowers/specs/2026-07-25-dhruvnext-ui-ux-design-reference.md`), D2 minimal-viable
 * scope only: a grouped list of the four calculator-tool sections (Borrowing/Growing/Tax &
 * salary/Everyday), each a single tappable row that hands off to [onOpenTool]. The search field
 * and "pick up where you left off" resume cards from the full §6.4 spec are later-phase scope
 * and are deliberately not built here.
 *
 * Renders only the scrollable content column — no `Scaffold`/`TopAppBar` — because Plan is a
 * top-level tab root (ADR-0024 §5: a `showNav` tab, not a detail route), so the tab host owns
 * any chrome for this screen.
 */
@Composable
fun PlanLauncher(
    onOpenTool: (PlanTool) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(DhruvNextSpacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(SECTION_GAP),
    ) {
        PlanSection(
            label = "Borrowing",
            row = {
                ListGroupRow(
                    title = "Loan EMI",
                    subtitle = "EMI, tenure & prepayment",
                    icon = Icons.Default.AccountBalance,
                    onClick = { onOpenTool(PlanTool.LOAN) },
                )
            },
        )
        PlanSection(
            label = "Growing",
            row = {
                ListGroupRow(
                    title = "SIP & returns",
                    subtitle = "SIP growth, XIRR",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    onClick = { onOpenTool(PlanTool.INVEST) },
                )
            },
        )
        PlanSection(
            label = "Tax & salary",
            row = {
                ListGroupRow(
                    title = "GST & salary",
                    subtitle = "GST, CTC to take-home",
                    icon = Icons.Default.Receipt,
                    onClick = { onOpenTool(PlanTool.TAX) },
                )
            },
        )
        PlanSection(
            label = "Everyday",
            row = {
                ListGroupRow(
                    title = "Everyday maths",
                    subtitle = "Interest, discount, tip split",
                    icon = Icons.Default.Calculate,
                    onClick = { onOpenTool(PlanTool.EVERYDAY) },
                )
            },
        )
    }
}

/** One [SectionLabel] + single-row [ListGroup] pair — DhruvNext §6.4's per-section grouping. */
@Composable
private fun PlanSection(
    label: String,
    row: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
    ) {
        SectionLabel(text = label)
        ListGroup(rows = listOf(row))
    }
}

/** Gap between Plan's four sections — Dhruv's "between sections" spacing scale. */
private val SECTION_GAP = 24.dp

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO)
@Composable
private fun PlanLauncherPreview() {
    DhruvTheme {
        PlanLauncher(onOpenTool = {})
    }
}
