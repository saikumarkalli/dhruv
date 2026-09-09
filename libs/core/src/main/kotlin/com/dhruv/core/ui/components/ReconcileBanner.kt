package com.dhruv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/**
 * A stale-balance prompt (D6/D7, FR-020) — "Reconciled 28 Jul · needs check" plus a fix action.
 * Rendered whenever an account's `reconciled_at` is older than [MoneyConfig.STALENESS_THRESHOLD_DAYS]
 * (caller resolves the threshold; this component only renders the already-decided state).
 */
@Composable
fun ReconcileBanner(
    message: String,
    onReconcile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DhruvNextRadii.innerTile))
                .background(colors.warnSoft)
                .padding(DhruvNextSpacing.interCardGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = colors.warn)
        Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(text = message, color = colors.warn, fontSize = DhruvNextType.body, fontWeight = FontWeight.Medium)
        }
        NxButton(text = "Reconcile", onClick = onReconcile, size = NxButtonSize.Small, variant = NxButtonVariant.Soft)
    }
}
