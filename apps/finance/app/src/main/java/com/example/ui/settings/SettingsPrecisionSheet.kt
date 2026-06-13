package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPrecisionSheet(
    currentPrecision: Int,
    onPrecisionSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Decimal Precision", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Choose how many decimal places results should show across the calculator.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            // Preview
            val previewNumber = 12.3456789
            val pattern = if (currentPrecision > 0) "#." + "#".repeat(currentPrecision) else "#"
            val df = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US))
            val formatted = df.format(previewNumber)
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("Preview", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("12.3456789", fontSize = 16.sp)
                    Text("becomes", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatted, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Precision", fontWeight = FontWeight.Bold)
            val precs = listOf(2, 3, 4, 5, 6, 8)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                precs.forEachIndexed { index, num ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = precs.size),
                        onClick = { onPrecisionSelected(num) },
                        selected = currentPrecision == num
                    ) {
                        Text("$num")
                    }
                }
            }
            
            Text("Current: $currentPrecision decimal places", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
