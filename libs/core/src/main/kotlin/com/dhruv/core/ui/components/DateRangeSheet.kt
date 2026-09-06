package com.dhruv.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import java.time.LocalDate

/** A named quick-pick range plus its resolved [start]/[end] (inclusive) dates. */
data class DateRangePreset(
    val label: String,
    val start: LocalDate,
    val end: LocalDate,
)

/**
 * A date-range picker sheet (design batch B2) — D5's amount/date filter. Offers named presets
 * (This month, Last month, Last 3 months) plus a custom start/end pair typed as ISO dates; no
 * calendar-grid widget is drawn by the design for this control, so this stays text-entry, not a
 * full calendar view.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeSheet(
    presets: List<DateRangePreset>,
    onDismissRequest: () -> Unit,
    onApply: (start: LocalDate, end: LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    initialStart: LocalDate? = null,
    initialEnd: LocalDate? = null,
) {
    val colors = LocalDhruvNextColors.current
    var startText by remember { mutableStateOf(initialStart?.toString().orEmpty()) }
    var endText by remember { mutableStateOf(initialEnd?.toString().orEmpty()) }

    DhruvModalSheet(onDismissRequest = onDismissRequest, modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(DhruvNextSpacing.screenGutter)) {
            Text(
                text = "Date range",
                color = colors.tx,
                fontSize = DhruvNextType.title,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = DhruvNextSpacing.interCardGap),
            )
            Row {
                presets.forEach { preset ->
                    Pill(
                        label = preset.label,
                        onClick = {
                            startText = preset.start.toString()
                            endText = preset.end.toString()
                        },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
            Row(modifier = Modifier.padding(top = DhruvNextSpacing.interCardGap)) {
                NxTextField(
                    value = startText,
                    onValueChange = { startText = it },
                    label = "From (YYYY-MM-DD)",
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                NxTextField(
                    value = endText,
                    onValueChange = { endText = it },
                    label = "To (YYYY-MM-DD)",
                    modifier = Modifier.weight(1f),
                )
            }
            NxButton(
                text = "Apply",
                onClick = {
                    val start = startText.toLocalDateOrNull()
                    val end = endText.toLocalDateOrNull()
                    if (start != null && end != null && !start.isAfter(end)) {
                        onApply(start, end)
                    }
                },
                block = true,
                modifier = Modifier.padding(top = DhruvNextSpacing.sectionGap),
            )
        }
    }
}

private fun String.toLocalDateOrNull(): LocalDate? =
    try {
        LocalDate.parse(this)
    } catch (e: java.time.format.DateTimeParseException) {
        null
    }
