package com.dhruv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/** Nothing-here-yet placeholder — used across Home/Insights until their backend data lands. */
@Composable
fun EmptyStateCard(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Info,
) {
    val colors = LocalDhruvNextColors.current
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DhruvNextRadii.card))
                .background(colors.surf2)
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = colors.tx3)
        Text(
            text = message,
            modifier = Modifier.padding(top = 8.dp),
            color = colors.tx2,
            fontSize = DhruvNextType.body,
            textAlign = TextAlign.Center,
        )
    }
}

/** A thin offline/cached-data disclosure strip — DhruvNext §6.6's currency-converter footer. */
@Composable
fun OfflineBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DhruvNextRadii.innerTile))
                .background(colors.surf2)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(imageVector = Icons.Default.CloudOff, contentDescription = null, tint = colors.tx3)
        Text(text = message, color = colors.tx2, fontSize = DhruvNextType.meta)
    }
}

/** An error state with a Retry action — network/repository failures. */
@Composable
fun RetryErrorCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryLabel: String = "Retry",
) {
    val colors = LocalDhruvNextColors.current
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DhruvNextRadii.card))
                .background(colors.surf2)
                .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, color = colors.tx2, fontSize = DhruvNextType.body, textAlign = TextAlign.Center)
        TextButton(onClick = onRetry) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = colors.acc)
            Text(text = retryLabel, color = colors.acc, fontWeight = FontWeight.Medium)
        }
    }
}
