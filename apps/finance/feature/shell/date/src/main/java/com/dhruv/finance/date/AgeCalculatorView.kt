package com.dhruv.finance.date

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.core.ui.components.NxCard
import com.dhruv.core.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AgeCalculatorView(viewModel: DateViewModel) {
    val context = LocalContext.current
    val sdfDisplay = SimpleDateFormat("MMM dd, yyyy", Locale.US)

    var birthDate by remember { mutableStateOf(Calendar.getInstance().apply { set(2000, 0, 1) }) }
    var referenceDate by remember { mutableStateOf(Calendar.getInstance()) }

    val ageResult =
        remember(birthDate, referenceDate) {
            viewModel.calculateAge(birthDate, referenceDate)
        }

    val ageYears = ageResult.years
    val ageMonths = ageResult.months
    val ageDays = ageResult.days
    val nextMonths = ageResult.nextMonths
    val nextDays = ageResult.nextDays
    val dayOfWeekOfNextBirthday = ageResult.dayOfWeekOfNextBirthday

    val totalDays = ageResult.totalDays
    val totalMonths = ageResult.totalMonths
    val totalWeeks = ageResult.totalWeeks
    val totalHours = ageResult.totalHours
    val totalMinutes = ageResult.totalMinutes

    val colors = LocalDhruvNextColors.current

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            DatePickerDialog(
                                context,
                                { _, y, m, d -> birthDate = Calendar.getInstance().apply { set(y, m, d) } },
                                birthDate.get(Calendar.YEAR),
                                birthDate.get(Calendar.MONTH),
                                birthDate.get(Calendar.DAY_OF_MONTH),
                            ).show()
                        }.padding(vertical = DhruvNextSpacing.interCardGap, horizontal = DhruvNextSpacing.screenGutter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Date of birth",
                    fontSize = DhruvNextType.body,
                    color = colors.tx,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sdfDisplay.format(birthDate.time),
                        fontSize = DhruvNextType.body,
                        fontWeight = FontWeight.Bold,
                        color = colors.acc,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select birth date",
                        tint = colors.tx2,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            HorizontalDivider(color = colors.line.copy(alpha = 0.5f), thickness = 0.5.dp)

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            DatePickerDialog(
                                context,
                                { _, y, m, d -> referenceDate = Calendar.getInstance().apply { set(y, m, d) } },
                                referenceDate.get(Calendar.YEAR),
                                referenceDate.get(Calendar.MONTH),
                                referenceDate.get(Calendar.DAY_OF_MONTH),
                            ).show()
                        }.padding(vertical = DhruvNextSpacing.interCardGap, horizontal = DhruvNextSpacing.screenGutter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Today",
                    fontSize = DhruvNextType.body,
                    color = colors.tx,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sdfDisplay.format(referenceDate.time),
                        fontSize = DhruvNextType.body,
                        fontWeight = FontWeight.Bold,
                        color = colors.tx2,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select reference date",
                        tint = colors.tx2,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // Output Result card
        NxCard(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DhruvNextSpacing.screenGutter, vertical = 8.dp),
            padding = 0.dp,
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = DhruvNextSpacing.sectionGap, horizontal = DhruvNextSpacing.screenGutter),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left block - Age
                Column(
                    modifier =
                        Modifier
                            .weight(1.2f)
                            .padding(end = 8.dp),
                ) {
                    Text(
                        text = "Age",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Normal,
                        color = colors.tx2,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = "$ageYears",
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Normal,
                            color = colors.acc,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "years",
                            fontSize = DhruvNextType.body,
                            color = colors.tx2,
                            modifier = Modifier.padding(bottom = 14.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "$ageMonths months | $ageDays days",
                        fontSize = DhruvNextType.body,
                        color = colors.tx2,
                    )
                }

                // Vertical Divider
                Box(
                    modifier =
                        Modifier
                            .width(1.dp)
                            .height(130.dp)
                            .background(colors.line),
                )

                // Right block - Next Birthday
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(start = DhruvNextSpacing.screenGutter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Next birthday",
                        fontSize = DhruvNextType.cardTitle,
                        fontWeight = FontWeight.Bold,
                        color = colors.acc,
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier =
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.acc),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cake,
                            contentDescription = "Cake icon",
                            tint = colors.onAcc,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = dayOfWeekOfNextBirthday,
                        fontSize = DhruvNextType.cardTitle,
                        color = colors.tx2,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "$nextMonths months | $nextDays days",
                        fontSize = DhruvNextType.body,
                        color = colors.tx2,
                    )
                }
            }

            HorizontalDivider(color = colors.line)

            // Summary heading and Grid
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp, horizontal = DhruvNextSpacing.screenGutter),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Summary",
                    fontSize = DhruvNextType.title,
                    fontWeight = FontWeight.Bold,
                    color = colors.acc,
                )

                Spacer(modifier = Modifier.height(DhruvNextSpacing.sectionGap))

                // Row 1: Years, Months, Weeks
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SummaryGridItem(
                        label = "Years",
                        value = "$ageYears",
                        modifier = Modifier.weight(1f),
                    )
                    SummaryGridItem(
                        label = "Months",
                        value = "$totalMonths",
                        modifier = Modifier.weight(1f),
                    )
                    SummaryGridItem(
                        label = "Weeks",
                        value = "$totalWeeks",
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Row 2: Days, Hours, Minutes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SummaryGridItem(
                        label = "Days",
                        value = "$totalDays",
                        modifier = Modifier.weight(1f),
                    )
                    SummaryGridItem(
                        label = "Hours",
                        value = "$totalHours",
                        modifier = Modifier.weight(1f),
                    )
                    SummaryGridItem(
                        label = "Minutes",
                        value = "$totalMinutes",
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(DhruvNextSpacing.sectionGap))

                Text(
                    text = "powered by Calculator",
                    fontSize = DhruvNextType.meta,
                    color = colors.tx3,
                )
            }
        }

        // Bottom Actions Button Bar
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DhruvNextSpacing.screenGutter, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
        ) {
            Button(
                onClick = { },
                modifier =
                    Modifier
                        .weight(1f)
                        .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.surf2),
                shape = RoundedCornerShape(DhruvNextRadii.pill),
            ) {
                Text(
                    text = "Add to Calendar",
                    color = colors.tx2,
                    fontSize = DhruvNextType.body,
                    fontWeight = FontWeight.Medium,
                )
            }

            Button(
                onClick = { },
                modifier =
                    Modifier
                        .weight(1f)
                        .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.acc),
                shape = RoundedCornerShape(DhruvNextRadii.pill),
            ) {
                Text(
                    text = "Share",
                    color = colors.onAcc,
                    fontSize = DhruvNextType.body,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
fun SummaryGridItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            fontSize = DhruvNextType.meta,
            color = colors.tx2,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = DhruvNextType.cardTitle,
            fontWeight = FontWeight.Bold,
            color = colors.tx,
        )
    }
}
