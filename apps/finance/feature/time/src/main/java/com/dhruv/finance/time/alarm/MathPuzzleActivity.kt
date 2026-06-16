package com.dhruv.finance.time.alarm

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.finance.time.service.alarm.AlarmService

class MathPuzzleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        val difficulty = intent.getIntExtra("ALARM_DIFFICULTY", 1)

        setContent {
            MathPuzzleScreen(difficulty = difficulty) {
                val stopIntent = Intent(this, AlarmService::class.java).apply {
                    action = "STOP_ALARM"
                }
                startService(stopIntent)
                finish()
            }
        }
    }
}

@Composable
fun MathPuzzleScreen(difficulty: Int, onSolved: () -> Unit) {
    val (num1, num2) = remember {
        when (difficulty) {
            0 -> (1..10).random() to (1..10).random()
            1 -> (10..50).random() to (10..50).random()
            else -> (50..200).random() to (50..200).random()
        }
    }
    val answer = num1 + num2
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Alarm Ringing!", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(32.dp))
            Text("Solve to dismiss:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text("$num1 + $num2 = ?", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedTextField(
                value = input,
                onValueChange = { input = it; error = false },
                isError = error,
                singleLine = true,
                label = { Text("Answer") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    if (input.toIntOrNull() == answer) {
                        onSolved()
                    } else {
                        error = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Dismiss Alarm")
            }
        }
    }
}
