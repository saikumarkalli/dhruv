package com.dhruv.finance.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.components.NxCard
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPrecisionSheet(
    currentPrecision: Int,
    onPrecisionSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalDhruvNextColors.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(DhruvNextSpacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
        ) {
            Text("Decimal Precision", fontSize = DhruvNextType.title, fontWeight = FontWeight.Bold, color = colors.tx)
            Text(
                "Choose how many decimal places results should show across the calculator.",
                fontSize = DhruvNextType.body,
                color = colors.tx2,
            )

            val previewNumber = 12.3456789
            val pattern = if (currentPrecision > 0) "#." + "#".repeat(currentPrecision) else "#"
            val df = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US))
            val formatted = df.format(previewNumber)

            NxCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Preview", fontWeight = FontWeight.Bold, color = colors.acc)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("12.3456789", fontSize = DhruvNextType.cardTitle, color = colors.tx)
                    Text("becomes", fontSize = DhruvNextType.body, color = colors.tx2)
                    Text(formatted, fontSize = DhruvNextType.title, fontWeight = FontWeight.Bold, color = colors.tx)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Precision", fontWeight = FontWeight.Bold, color = colors.tx)
            val precs = listOf(2, 3, 4, 5, 6, 8)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                precs.forEachIndexed { index, num ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = precs.size),
                        onClick = { onPrecisionSelected(num) },
                        selected = currentPrecision == num,
                    ) {
                        Text("$num")
                    }
                }
            }

            Text("Current: $currentPrecision decimal places", fontSize = DhruvNextType.body, color = colors.tx2)
            Spacer(modifier = Modifier.height(DhruvNextSpacing.sectionGap))
        }
    }
}
