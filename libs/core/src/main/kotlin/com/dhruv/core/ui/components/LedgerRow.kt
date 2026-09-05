package com.dhruv.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/**
 * One ledger row (D1, FR-012) — payee/description, category · account, and a signed [MoneyText]
 * amount (positive = income, tinted [com.dhruv.core.ui.theme.DhruvNextColors.pos]).
 */
@Composable
fun LedgerRow(
    title: String,
    subtitle: String,
    amountPaise: Long,
    isPositive: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = LocalDhruvNextColors.current
    val amountColor = if (isPositive) colors.pos else colors.tx
    val amountDescription = if (isPositive) "credit" else "debit"
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .let { if (onClick != null) it.clickable(onClick = onClick) else it }
                .padding(horizontal = 4.dp, vertical = 10.dp)
                .semantics { contentDescription = "$title, $subtitle, $amountDescription" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.tx,
                fontSize = DhruvNextType.cardTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = subtitle, color = colors.tx2, fontSize = DhruvNextType.meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        MoneyText(
            paise = amountPaise,
            variant = MoneyTextVariant.Row,
            color = amountColor,
        )
    }
}
