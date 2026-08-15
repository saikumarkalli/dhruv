package com.dhruv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

/**
 * Full-card state — a network-backed screen (Home, Money, Plan-live modules, Insights) whose
 * session is signed out (NFR-4's signed-out/offline/not-configured trio, spec §2.2/§5). Distinct
 * from [RetryErrorCard] — this is not a failure, it is the design's explicit A2 "Use offline —
 * calculators only" path playing out on a tracker screen.
 */
@Composable
fun SignedOutCard(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
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
        stateIcon(Icons.Default.Lock, colors.tx3, colors.surf)
        Text(
            text = message,
            modifier = Modifier.padding(top = 12.dp),
            color = colors.tx2,
            fontSize = DhruvNextType.body,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onAction,
            modifier = Modifier.padding(top = 16.dp),
            shape = RoundedCornerShape(DhruvNextRadii.innerTile),
            colors = ButtonDefaults.buttonColors(containerColor = colors.acc, contentColor = colors.onAcc),
        ) {
            Text(actionLabel)
        }
    }
}

/**
 * Full-card state — device has no connection and the screen has nothing cached to show yet.
 * A screen with cached data prefers [OfflineBanner] (a strip above still-visible content) over
 * this full-card replacement.
 */
@Composable
fun OfflineStateCard(
    message: String = "You're offline. This screen needs a connection.",
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
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
        stateIcon(Icons.Default.WifiOff, colors.tx3, colors.surf)
        Text(
            text = message,
            modifier = Modifier.padding(top = 12.dp),
            color = colors.tx2,
            fontSize = DhruvNextType.body,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            TextButton(onClick = onRetry) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = colors.acc)
                Text(text = "Retry", color = colors.acc, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/**
 * Full-card state — the surface is signed in and online, but the feature behind it hasn't shipped
 * yet (this build's Money/Insights tabs during Phase 0, before their real screens land). Distinct
 * from [FeatureDisabledCard] (`FeatureHost.kt`) which fires on a remote-config flag flip for a
 * feature that DOES exist; this fires for a route with no implementation at all.
 */
@Composable
fun NotConfiguredCard(
    message: String,
    modifier: Modifier = Modifier,
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
        stateIcon(Icons.Default.Build, colors.tx3, colors.surf)
        Text(
            text = message,
            modifier = Modifier.padding(top = 12.dp),
            color = colors.tx2,
            fontSize = DhruvNextType.body,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun stateIcon(
    icon: ImageVector,
    tint: Color,
    background: Color,
) {
    Box(
        modifier = Modifier.size(48.dp).clip(CircleShape).background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint)
    }
}
