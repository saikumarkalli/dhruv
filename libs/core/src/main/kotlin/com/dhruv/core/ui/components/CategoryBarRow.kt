package com.dhruv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/**
 * DhruvNext §6.2's "Where it went" / §6.5's "Top merchants" row: icon + label + amount + a
 * proportional bar underneath, sized by [fraction] (0f–1f, this category's share of the max).
 */
@Composable
fun CategoryBarRow(
    label: String,
    amountText: String,
    fraction: Float,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = LocalDhruvNextColors.current
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, tint = colors.tx2, modifier = Modifier.padding(end = 8.dp))
                }
                Text(text = label, color = colors.tx, fontSize = 13.5f.sp, fontWeight = FontWeight.Medium)
            }
            Text(text = amountText, color = colors.tx, fontSize = 13.5f.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.surf2),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(colors.acc),
            )
        }
    }
}
