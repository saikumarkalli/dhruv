package com.dhruv.finance.date

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.core.ui.components.NxCard
import com.dhruv.core.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DateDifferenceView(viewModel: DateViewModel) {
    val colors = LocalDhruvNextColors.current
    val context = LocalContext.current
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    var date1 by remember { mutableStateOf(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -10) }) }
    var date2 by remember { mutableStateOf(Calendar.getInstance()) }

    val diffResult =
        remember(date1, date2) {
            viewModel.calculateDifference(date1, date2)
        }

    val totalDays = diffResult.totalDays
    val yearsParts = diffResult.years
    val monthsParts = diffResult.months
    val remainingDaysParts = diffResult.days

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Evaluate the distance between two dates", fontSize = DhruvNextType.cardTitle, fontWeight = FontWeight.Bold, color = colors.tx)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Start Date Input
            Column(modifier = Modifier.weight(1f)) {
                Text("Start Date", fontSize = DhruvNextType.meta, color = colors.tx3)
                NxCard(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d -> date1 = Calendar.getInstance().apply { set(y, m, d) } },
                                    date1.get(Calendar.YEAR),
                                    date1.get(Calendar.MONTH),
                                    date1.get(Calendar.DAY_OF_MONTH),
                                ).show()
                            },
                ) {
                    Text(
                        text = sdf.format(date1.time),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // End Date Input
            Column(modifier = Modifier.weight(1f)) {
                Text("End Date", fontSize = DhruvNextType.meta, color = colors.tx3)
                NxCard(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d -> date2 = Calendar.getInstance().apply { set(y, m, d) } },
                                    date2.get(Calendar.YEAR),
                                    date2.get(Calendar.MONTH),
                                    date2.get(Calendar.DAY_OF_MONTH),
                                ).show()
                            },
                ) {
                    Text(
                        text = sdf.format(date2.time),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // Output Result card
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(colors.accSoft, RoundedCornerShape(DhruvNextRadii.card))
                    .padding(DhruvNextSpacing.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Primary Duration Metric",
                fontSize = DhruvNextType.meta,
                color = colors.tx2,
            )
            Text(
                "$totalDays Days",
                fontSize = DhruvNextType.title,
                fontWeight = FontWeight.Black,
                color = colors.acc,
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = colors.line,
            )

            Text(
                "Equivalent Chronological Breakdown",
                fontSize = DhruvNextType.meta,
                color = colors.tx2,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DateStatChip(valStr = "$yearsParts", denomStr = "Years")
                DateStatChip(valStr = "$monthsParts", denomStr = "Months")
                DateStatChip(valStr = "$remainingDaysParts", denomStr = "Days")
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Weeks equivalent: ${(totalDays / 7)} Weeks, ${(totalDays % 7)} Days",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.tx,
            )
        }
    }
}

@Composable
fun DateStatChip(
    valStr: String,
    denomStr: String,
) {
    val colors = LocalDhruvNextColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .background(colors.surf2, RoundedCornerShape(DhruvNextRadii.innerTile))
                .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(valStr, fontSize = DhruvNextType.cardTitle, fontWeight = FontWeight.Bold, color = colors.tx)
        Text(denomStr, fontSize = DhruvNextType.sectionLabel, color = colors.tx3)
    }
}
