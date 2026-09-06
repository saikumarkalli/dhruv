package com.dhruv.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.dhruv.core.format.Paise
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import java.math.BigDecimal
import java.math.RoundingMode

private const val MAX_RUPEE_DIGITS = 9

/**
 * An amount-entry sheet — [NumericKeypad] inside [DhruvModalSheet] with a rupee display and a
 * Save key (design batch B2/D2's amount pad). This is a *composition* of two already-built
 * components, not a second keypad implementation (constitution Article VI, research R5).
 * [onSave] receives the entered amount as integer paise (Article VII — never a `Double`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountKeypadSheet(
    onDismissRequest: () -> Unit,
    onSave: (paise: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var digits by remember { mutableStateOf("") }
    val colors = LocalDhruvNextColors.current
    val rupees = if (digits.isEmpty()) BigDecimal.ZERO else BigDecimal(digits).movePointLeft(2)
    val paise = rupees.setScale(2, RoundingMode.HALF_UP).movePointRight(2).toLong()

    DhruvModalSheet(onDismissRequest = onDismissRequest, modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(DhruvNextSpacing.screenGutter)) {
            Text(
                text = Paise.format(paise),
                color = colors.tx,
                fontSize = DhruvNextType.hero,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = DhruvNextSpacing.sectionGap),
            )
            NumericKeypad(
                rows = amountKeypadRows(),
                onKeyPress = { key ->
                    when (key) {
                        "⌫" -> digits = digits.dropLast(1)
                        "Save" -> if (paise > 0) onSave(paise)
                        else -> if (digits.length < MAX_RUPEE_DIGITS) digits += key
                    }
                },
            )
        }
    }
}

private fun amountKeypadRows(): List<List<KeypadKey>> =
    listOf(
        listOf(KeypadKey("1"), KeypadKey("2"), KeypadKey("3")),
        listOf(KeypadKey("4"), KeypadKey("5"), KeypadKey("6")),
        listOf(KeypadKey("7"), KeypadKey("8"), KeypadKey("9")),
        listOf(
            KeypadKey("0"),
            KeypadKey("00"),
            KeypadKey(label = "⌫", icon = Icons.AutoMirrored.Filled.Backspace),
        ),
        listOf(KeypadKey(label = "Save", span = 3, fillAccent = true, solidAccent = true)),
    )
