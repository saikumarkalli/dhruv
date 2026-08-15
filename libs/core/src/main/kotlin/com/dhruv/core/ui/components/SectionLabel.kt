package com.dhruv.core.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import java.util.Locale

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
        fontFamily = FontFamily.Monospace,
        letterSpacing = 0.8.sp,
    )
}
