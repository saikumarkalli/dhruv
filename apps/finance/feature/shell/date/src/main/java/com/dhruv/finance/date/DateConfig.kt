package com.dhruv.finance.date

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

data class DateCalcItem(
    val name: String,
    val icon: ImageVector,
    val description: String,
)

val DateCalcItems =
    listOf(
        DateCalcItem("Date Difference", Icons.Default.DateRange, "Find duration between two calendar dates."),
        DateCalcItem("Add / Subtract Days", Icons.Default.Event, "Move calendar dates forwards or backwards in time."),
        DateCalcItem("Age Calculator", Icons.Default.Cake, "Breakdown of years, months, days, and next birthday countdown."),
        DateCalcItem("Countdown Tracker", Icons.Default.Timelapse, "Live countdown of days and hours to absolute goals."),
        DateCalcItem("Time Zone Converter", Icons.Default.Language, "Quick conversion between world coordinate UTC positions."),
        DateCalcItem("Business Working Days", Icons.Default.Work, "Count weekdays excluding standard Saturdays & Sundays."),
        DateCalcItem("Unix Epoch Converter", Icons.Default.Terminal, "Parse integer timestamp seconds to UTC format and vice versa."),
    )
