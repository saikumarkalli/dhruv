package com.dhruv.finance.date

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit
import com.dhruv.core.ui.theme.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.collectAsState
import com.dhruv.settings.SettingsRepository
import org.koin.compose.koinInject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Composable
fun AgeCalculatorView(viewModel: DateViewModel) {
    val context = LocalContext.current
    val sdfDisplay = SimpleDateFormat("MMM dd, yyyy", Locale.US)

    var birthDate by remember { mutableStateOf(Calendar.getInstance().apply { set(2000, 0, 1) }) }
    var referenceDate by remember { mutableStateOf(Calendar.getInstance()) }

    val ageResult = remember(birthDate, referenceDate) {
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

    val miuiOrange = Color(0xFFFF5C00) // Deep Premium MIUI-style Orange
    val textDark = Color(0xFF212121)   // Dark Grey/Black for values
    val textGrey = Color(0xFF757575)   // Muted Grey for labels
    val dividerGrey = Color(0xFFE5E5E5) // Light line separation grey

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Date Selectors exactly like the screenshot
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> birthDate = Calendar.getInstance().apply { set(y, m, d) } },
                            birthDate.get(Calendar.YEAR), birthDate.get(Calendar.MONTH), birthDate.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Date of birth",
                    style = MaterialTheme.typography.bodyLarge,
                    color = textDark
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sdfDisplay.format(birthDate.time),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = miuiOrange
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select birth date",
                        tint = textGrey,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = dividerGrey.copy(alpha = 0.5f), thickness = 0.5.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> referenceDate = Calendar.getInstance().apply { set(y, m, d) } },
                            referenceDate.get(Calendar.YEAR), referenceDate.get(Calendar.MONTH), referenceDate.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.bodyLarge,
                    color = textDark
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sdfDisplay.format(referenceDate.time),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = textGrey
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select reference date",
                        tint = textGrey,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Output Result card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, dividerGrey),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left block - Age
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Age",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Normal,
                                fontSize = 32.sp
                            ),
                            color = textGrey
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "$ageYears",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 72.sp
                                ),
                                color = miuiOrange
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "years",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textGrey,
                                modifier = Modifier.padding(bottom = 14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "$ageMonths months | $ageDays days",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textGrey
                        )
                    }

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(130.dp)
                            .background(dividerGrey)
                    )

                    // Right block - Next Birthday
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Next birthday",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = miuiOrange
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(miuiOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cake,
                                contentDescription = "Cake icon",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = dayOfWeekOfNextBirthday,
                            style = MaterialTheme.typography.titleMedium,
                            color = textGrey
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "$nextMonths months | $nextDays days",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textGrey
                        )
                    }
                }

                HorizontalDivider(color = dividerGrey)

                // Summary heading and Grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Summary",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = miuiOrange
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Row 1: Years, Months, Weeks
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SummaryGridItem(
                            label = "Years",
                            value = "$ageYears",
                            modifier = Modifier.weight(1f),
                            textGrey = textGrey,
                            textDark = textDark
                        )
                        SummaryGridItem(
                            label = "Months",
                            value = "$totalMonths",
                            modifier = Modifier.weight(1f),
                            textGrey = textGrey,
                            textDark = textDark
                        )
                        SummaryGridItem(
                            label = "Weeks",
                            value = "$totalWeeks",
                            modifier = Modifier.weight(1f),
                            textGrey = textGrey,
                            textDark = textDark
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Row 2: Days, Hours, Minutes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SummaryGridItem(
                            label = "Days",
                            value = "$totalDays",
                            modifier = Modifier.weight(1f),
                            textGrey = textGrey,
                            textDark = textDark
                        )
                        SummaryGridItem(
                            label = "Hours",
                            value = "$totalHours",
                            modifier = Modifier.weight(1f),
                            textGrey = textGrey,
                            textDark = textDark
                        )
                        SummaryGridItem(
                            label = "Minutes",
                            value = "$totalMinutes",
                            modifier = Modifier.weight(1f),
                            textGrey = textGrey,
                            textDark = textDark
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "powered by Calculator",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = textGrey.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Bottom Actions Button Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5F5F5)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = "Add to Calendar",
                    color = textGrey,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                )
            }

            Button(
                onClick = { },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = miuiOrange),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = "Share",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
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
    textGrey: Color,
    textDark: Color
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textGrey
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = textDark
        )
    }
}
