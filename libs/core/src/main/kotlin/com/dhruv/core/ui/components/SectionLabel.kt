package com.dhruv.core.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import java.util.Locale

/** Uppercase section header — DhruvNext §4: 10sp/700, 1.1sp letter-spacing. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    Text(
        text = text.uppercase(Locale.getDefault()),
        modifier = modifier,
        color = colors.tx3,
        fontSize = DhruvNextType.sectionLabel,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.1.sp,
    )
}
