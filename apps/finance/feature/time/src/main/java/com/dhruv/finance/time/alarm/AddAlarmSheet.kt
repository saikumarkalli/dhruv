package com.dhruv.finance.time.alarm

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import android.widget.Toast
import androidx.compose.ui.unit.dp
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmSheet(
    onDismiss: () -> Unit,
    onSave: (timeInMillis: Long, label: String, difficulty: Int) -> Unit
) {
    val context = LocalContext.current
    val currentTime = Calendar.getInstance()
    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
        initialMinute = currentTime.get(Calendar.MINUTE),
        is24Hour = false
    )

    var label by remember { mutableStateOf("Wake Up") }
    var difficulty by remember { mutableIntStateOf(1) } // 0: Easy, 1: Medium, 2: Hard

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Set Alarm", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(24.dp))

            TimePicker(state = timePickerState)

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Alarm Label") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Math Puzzle Difficulty", modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            val difficulties = listOf("Easy", "Medium", "Hard")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                difficulties.forEachIndexed { index, text ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = difficulties.size),
                        onClick = { difficulty = index },
                        selected = difficulty == index
                    ) {
                        Text(text)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    val target = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    if (target.before(Calendar.getInstance())) {
                        target.add(Calendar.DATE, 1)
                    }

                    val diff = target.timeInMillis - System.currentTimeMillis()
                    val hours = diff / 3600000
                    val minutes = (diff % 3600000) / 60000

                    val toastMsg = if (hours > 0) {
                        "Alarm set for $hours hours and $minutes minutes from now"
                    } else {
                        "Alarm set for $minutes minutes from now"
                    }
                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()

                    onSave(target.timeInMillis, label.ifBlank { "Alarm" }, difficulty)
                    onDismiss()
                }) {
                    Text("Save")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
