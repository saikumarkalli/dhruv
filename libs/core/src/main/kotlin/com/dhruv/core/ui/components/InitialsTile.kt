package com.dhruv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import java.util.Locale

/**
 * Currency/merchant/person avatar without a real image — DhruvNext §4: "never an emoji or
 * generic icon." Rounded [DhruvNextColors.surf2] square with the name's initials.
 */
@Composable
fun InitialsTile(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
) {
    val colors = LocalDhruvNextColors.current
    Box(
        modifier =
            modifier
                .size(size)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surf2),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initialsFor(name),
            color = colors.tx2,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** First letter of the first two words, or the first two letters of a single word. */
fun initialsFor(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val initials =
        when {
            words.isEmpty() -> ""
            words.size >= 2 -> "${words[0].first()}${words[1].first()}"
            else -> words[0].take(2)
        }
    return initials.uppercase(Locale.getDefault())
}
