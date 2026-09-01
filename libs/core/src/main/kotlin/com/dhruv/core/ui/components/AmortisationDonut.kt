package com.dhruv.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/**
 * C7's loan payoff-progress donut (design system B3) — three fixed segments (paid principal, paid
 * interest, remaining) rather than an arbitrary [DonutSegment] list, since a loan always has
 * exactly these three. [centerLabel] is the caller's pre-formatted "84 of 180 paid" line (never
 * computed here — same "no formatting inside `:libs:core`" contract as [MoneyText]).
 */
@Composable
fun AmortisationDonut(
    principalPaidPaise: Long,
    interestPaidPaise: Long,
    remainingPaise: Long,
    centerLabel: String,
    modifier: Modifier = Modifier.size(160.dp),
    strokeWidth: Dp = 22.dp,
) {
    val colors = LocalDhruvNextColors.current
    val segments =
        listOf(
            DonutSegment("Principal paid", principalPaidPaise.toFloat(), "", colors.acc),
            DonutSegment("Interest paid", interestPaidPaise.toFloat(), "", colors.chart5),
            DonutSegment("Remaining", remainingPaise.toFloat(), "", colors.surf2),
        )
    DonutChart(segments = segments, modifier = modifier, strokeWidth = strokeWidth) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = centerLabel,
                color = colors.tx,
                fontSize = DhruvNextType.cardTitle,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}
