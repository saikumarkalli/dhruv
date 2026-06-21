package com.dhruv.finance.app.ui.settings

data class SettingsSectionConfig(
    val id: String,
    val title: String,
    val enabled: Boolean,
    val canDisable: Boolean,
    val tools: List<String>
)

data class AccentTarget(
    val id: String,
    val label: String,
    val selectedColorId: String
)

object SettingsConstants {
    val CONVERTER_TOOLS = listOf(
        "Age", "Area", "BMI", "Currency", "Data", "Date", "Discount",
        "Length", "Mass", "Numeral system", "Speed", "Temperature", "Time", "Volume"
    )

    val DATE_TOOLS = listOf(
        "Date Difference", "Add / Subtract Days", "Age Calculator",
        "Countdown Tracker", "Time Zone Converter", "Business Working Days", "Unix Epoch Converter"
    )

    val FINANCE_TOOLS = listOf(
        "Loan EMI", "Simple & Compound", "SIP Growth", "ROI / CAGR",
        "GST / Tax", "Discount & Markup", "Tip & Bill Split", "Salary Breakup",
        "Inflation Adjusted", "FD / RD Maturity"
    )

    val TIME_TOOLS = listOf(
        "Stopwatch", "Timer"
    )
}
