package com.dhruv.core.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/**
 * A day-group header for a ledger list (D1, FR-012) — the date on the left, that day's net amount
 * on the right. [netText] is already formatted by the caller via [MoneyText]/[com.dhruv.core.format.Paise]
 * so this component stays money-format-agnostic.
 */
@Composable
fun DayGroupHeader(
    dateLabel: String,
    modifier: Modifier = Modifier,
    netText: String? = null,
) {
    val colors = LocalDhruvNextColors.current
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        Text(
            text = dateLabel,
            color = colors.tx2,
            fontSize = DhruvNextType.sectionLabel,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (netText != null) {
            Text(text = netText, color = colors.tx3, fontSize = DhruvNextType.sectionLabel, fontWeight = FontWeight.Medium)
        }
    }
}
