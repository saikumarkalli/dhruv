// Named for the composable it hosts, not the small data class it also declares.
@file:Suppress("MatchingDeclarationName")

package com.dhruv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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

/** One "what's sent / what's never sent" info row on a [ConsentGateScaffold]. */
data class ConsentInfoRow(
    val label: String,
    val detail: String,
)

/**
 * DhruvNext §6.8's `consent` sheet content: icon, headline, body, info rows (what is/isn't sent),
 * Allow/Decline actions, optional footer. Content only — wrap in [DhruvModalSheet] at the call site.
 */
@Composable
fun ConsentGateScaffold(
    headline: String,
    body: String,
    onAllow: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.AutoAwesome,
    allowLabel: String = "Allow and continue",
    declineLabel: String = "Stay fully offline",
    infoRows: List<ConsentInfoRow> = emptyList(),
    footer: String? = null,
) {
    val colors = LocalDhruvNextColors.current
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(colors.accSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = colors.acc)
        }
        Text(
            text = headline,
            modifier = Modifier.padding(top = 16.dp),
            color = colors.tx,
            fontSize = DhruvNextType.title,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            modifier = Modifier.padding(top = 8.dp),
            color = colors.tx2,
            fontSize = DhruvNextType.body,
            textAlign = TextAlign.Center,
        )
        if (infoRows.isNotEmpty()) {
            Column(
                modifier =
                    Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(DhruvNextRadii.innerTile))
                        .background(colors.surf2)
                        .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                infoRows.forEach { row ->
                    Column {
                        Text(text = row.label, color = colors.tx, fontSize = DhruvNextType.body, fontWeight = FontWeight.Medium)
                        Text(text = row.detail, color = colors.tx2, fontSize = DhruvNextType.meta)
                    }
                }
            }
        }
        Button(
            onClick = onAllow,
            modifier = Modifier.padding(top = 20.dp).fillMaxWidth(),
            shape = RoundedCornerShape(DhruvNextRadii.innerTile),
            colors = ButtonDefaults.buttonColors(containerColor = colors.acc, contentColor = colors.onAcc),
        ) {
            Text(allowLabel)
        }
        OutlinedButton(
            onClick = onDecline,
            modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
            shape = RoundedCornerShape(DhruvNextRadii.innerTile),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.tx2),
        ) {
            Text(declineLabel)
        }
        if (footer != null) {
            Text(
                text = footer,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                color = colors.tx3,
                fontSize = DhruvNextType.meta,
                textAlign = TextAlign.Center,
            )
        }
    }
}
