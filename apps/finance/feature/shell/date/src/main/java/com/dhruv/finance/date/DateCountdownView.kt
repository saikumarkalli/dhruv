package com.dhruv.finance.date

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.components.NxCard
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun DateCountdownView() {
    val context = LocalContext.current
    val colors = LocalDhruvNextColors.current
    var targetDate by remember { mutableStateOf(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 45) }) }
    var currentMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentMillis = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }

    val finalDiffMs =
        remember(targetDate, currentMillis) {
            val target = targetDate.timeInMillis
            val diff = target - currentMillis
            if (diff < 0) 0L else diff
        }

    val days = TimeUnit.MILLISECONDS.toDays(finalDiffMs)
    val hours = TimeUnit.MILLISECONDS.toHours(finalDiffMs) % 24
    val minutes = TimeUnit.MILLISECONDS.toMinutes(finalDiffMs) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(finalDiffMs) % 60

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
    ) {
        Text("Target Calendar Event Timeline", fontSize = DhruvNextType.cardTitle, fontWeight = FontWeight.Bold, color = colors.tx)

        NxCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("Pick Target Goal Date", fontSize = DhruvNextType.meta, color = colors.tx3)
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d -> targetDate = Calendar.getInstance().apply { set(y, m, d) } },
                                    targetDate.get(Calendar.YEAR),
                                    targetDate.get(Calendar.MONTH),
                                    targetDate.get(Calendar.DAY_OF_MONTH),
                                ).show()
                            },
                    colors = CardDefaults.cardColors(containerColor = colors.surf),
                ) {
                    Row(
                        modifier = Modifier.padding(DhruvNextSpacing.inputGroupGap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = colors.acc)
                        Spacer(modifier = Modifier.width(DhruvNextSpacing.inputGroupGap))
                        Text(
                            text = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.US).format(targetDate.time),
                            fontWeight = FontWeight.Bold,
                            color = colors.tx,
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.accSoft),
            shape = RoundedCornerShape(DhruvNextRadii.card),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Time Remaining To Event Indicator",
                    fontSize = DhruvNextType.meta,
                    color = colors.tx2,
                )
                Spacer(modifier = Modifier.height(DhruvNextSpacing.inputGroupGap))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CountdownBlock(value = "%02d".format(days), label = "Days")
                    Text(":", fontWeight = FontWeight.Bold, fontSize = DhruvNextType.title, color = colors.tx)
                    CountdownBlock(value = "%02d".format(hours), label = "Hrs")
                    Text(":", fontWeight = FontWeight.Bold, fontSize = DhruvNextType.title, color = colors.tx)
                    CountdownBlock(value = "%02d".format(minutes), label = "Mins")
                    Text(":", fontWeight = FontWeight.Bold, fontSize = DhruvNextType.title, color = colors.tx)
                    CountdownBlock(value = "%02d".format(seconds), label = "Secs")
                }
            }
        }
    }
}

@Composable
fun CountdownBlock(
    value: String,
    label: String,
) {
    val colors = LocalDhruvNextColors.current
    Column(
        modifier =
            Modifier
                .background(colors.surf, RoundedCornerShape(DhruvNextRadii.innerTile))
                .padding(DhruvNextSpacing.inputGroupGap)
                .width(50.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            fontSize = DhruvNextType.title,
            fontWeight = FontWeight.Black,
            color = colors.acc,
        )
        Text(text = label, fontSize = DhruvNextType.sectionLabel, color = colors.tx3, fontWeight = FontWeight.Bold)
    }
}
