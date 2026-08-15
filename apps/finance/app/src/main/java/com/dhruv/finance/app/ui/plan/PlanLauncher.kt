package com.dhruv.finance.app.ui.plan

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dhruv.core.navigation.PlanTool
import com.dhruv.core.ui.components.ListGroup
import com.dhruv.core.ui.components.ListGroupRow
import com.dhruv.core.ui.components.SectionLabel
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvTheme

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
        verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.sectionGap),
    ) {
        PlanSections.forEach { section ->
            PlanSectionGroup(
                label = section.label,
                row = {
                    ListGroupRow(
                        title = section.title,
                        subtitle = section.subtitle,
                        icon = section.icon,
                        onClick = { onOpenTool(section.tool) },
                    )
                },
            )
        }
    }
}

@Composable
private fun PlanSectionGroup(
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

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO)
@Composable
private fun PlanLauncherPreview() {
    DhruvTheme {
        PlanLauncher(onOpenTool = {})
    }
}
