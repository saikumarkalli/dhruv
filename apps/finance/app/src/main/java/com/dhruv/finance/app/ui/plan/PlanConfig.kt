package com.dhruv.finance.app.ui.plan

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.ui.graphics.vector.ImageVector
import com.dhruv.core.navigation.PlanTool

data class PlanSectionItem(
    val label: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tool: PlanTool,
)

val PlanSections = listOf(
    PlanSectionItem("Borrowing", "Loan EMI", "EMI, tenure & prepayment", Icons.Default.AccountBalance, PlanTool.LOAN),
    PlanSectionItem("Growing", "SIP & returns", "SIP growth, XIRR", Icons.AutoMirrored.Filled.TrendingUp, PlanTool.INVEST),
    PlanSectionItem("Tax & salary", "GST & salary", "GST, CTC to take-home", Icons.Default.Receipt, PlanTool.TAX),
    PlanSectionItem("Everyday", "Everyday maths", "Interest, discount, tip split", Icons.Default.Calculate, PlanTool.EVERYDAY),
)
