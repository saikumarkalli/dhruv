package com.example.ui.calculator

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HistoryEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Dynamic Premium Color Palette Values matching prompt
val PremiumPrimaryAccent = Color(0xFFFF7433)
val PremiumAccentHover = Color(0xFFF56F3B)
val PremiumBackground = Color(0xFFFFFFFF)
val PremiumPrimaryText = Color(0xFF222222)
val PremiumSecondaryText = Color(0xFF666666)
val PremiumDivider = Color(0xFFEEEEEE)

// Dark Theme Adapters
val PremiumBackgroundDark = Color(0xFF151515)
val PremiumPrimaryTextDark = Color(0xFFFAFAFA)
val PremiumSecondaryTextDark = Color(0xFFB0B0B0)
val PremiumDividerDark = Color(0xFF2E2E2E)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val input by viewModel.input.collectAsState()
    val result by viewModel.result.collectAsState()
    val activeHistory by viewModel.activeHistory.collectAsState()
    val recycleBinHistory by viewModel.recycleBinHistory.collectAsState()
    val isDegree by viewModel.isDegree.collectAsState()
    val isHistoryLocked by viewModel.isHistoryLocked.collectAsState()
    val historyPinCode by viewModel.historyPinCode.collectAsState()

    var showHistoryScreen by remember { mutableStateOf(false) }
    var isHistoryUnlocked by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()
    val themeBgColor = MaterialTheme.colorScheme.background
    val themeTextColor = if (isDark) Color.White else Color(0xFF222222)
    val themeSecText = if (isDark) Color(0xFFB0B0B0) else Color(0xFF666666)
    val themeDivider = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

    LaunchedEffect(showHistoryScreen) {
        if (!showHistoryScreen) {
            isHistoryUnlocked = false
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.screenWidthDp >= 600

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(themeBgColor)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Main Column Area
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {


                // SCREEN INNER DISPLAY AREA
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.End
                ) {
                    // Cursor blinking animation
                    val infiniteTransition = rememberInfiniteTransition(label = "Blink")
                    val cursorAlpha by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 500),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "CursorAlpha"
                    )

                    val displayInput = input.ifEmpty { "0" }
                    val inputFontSize = when {
                        displayInput.length > 16 -> 28.sp
                        displayInput.length > 10 -> 36.sp
                        else -> 48.sp
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calc_input_field"),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayInput,
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontSize = inputFontSize,
                                fontWeight = FontWeight.Medium
                            ),
                            color = themeTextColor,
                            textAlign = TextAlign.End,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(if (displayInput.length > 16) 24.dp else if (displayInput.length > 10) 32.dp else 42.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = cursorAlpha))
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (result.isNotEmpty()) {
                        Text(
                            text = if (result.startsWith("=")) result else "= $result",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = themeSecText,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("calc_result_field")
                        )
                    }
                }

                // Subtle visual horizontal separator
                HorizontalDivider(
                    color = themeDivider,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // KEYPAD AREA
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp)
                        .align(Alignment.CenterHorizontally)
                        .background(themeDivider)
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    // Row 1: C, Backspace, %, /
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        SimpleKey(text = "C", tag = "key_btn_C", modifier = Modifier.weight(1f), onClick = { viewModel.onKeyPress("C") })
                        SimpleKey(icon = Icons.Default.Backspace, tag = "key_btn_⌫", modifier = Modifier.weight(1f), onClick = { viewModel.onKeyPress("⌫") })
                        SimpleKey(text = "%", tag = "key_btn_%", modifier = Modifier.weight(1f), isOperator = true, onClick = { viewModel.onKeyPress("%") })
                        SimpleKey(text = "/", tag = "key_btn_÷", modifier = Modifier.weight(1f), isOperator = true, onClick = { viewModel.onKeyPress("÷") })
                    }
                    // Row 2: 7, 8, 9, *
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        SimpleKey(text = "7", tag = "key_btn_7", modifier = Modifier.weight(1f), onClick = { viewModel.onKeyPress("7") })
                        SimpleKey(text = "8", tag = "key_btn_8", modifier = Modifier.weight(1f), onClick = { viewModel.onKeyPress("8") })
                        SimpleKey(text = "9", tag = "key_btn_9", modifier = Modifier.weight(1f), onClick = { viewModel.onKeyPress("9") })
                        SimpleKey(text = "*", tag = "key_btn_×", modifier = Modifier.weight(1f), isOperator = true, onClick = { viewModel.onKeyPress("×") })
                    }
                    // Row 3: 4, 5, 6, -
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        SimpleKey(text = "4", tag = "key_btn_4", modifier = Modifier.weight(1f), onClick = { viewModel.onKeyPress("4") })
                        SimpleKey(text = "5", tag = "key_btn_5", modifier = Modifier.weight(1f), onClick = { viewModel.onKeyPress("5") })
                        SimpleKey(text = "6", tag = "key_btn_6", modifier = Modifier.weight(1f), onClick = { viewModel.onKeyPress("6") })
                        SimpleKey(text = "-", tag = "key_btn_-", modifier = Modifier.weight(1f), isOperator = true, onClick = { viewModel.onKeyPress("-") })
                    }
                    // Row 4: 1, 2, 3, +
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        SimpleKey(text = "1", tag = "key_btn_1", modifier = Modifier.weight(1f), onClick = { viewModel.onKeyPress("1") })
                        SimpleKey(text = "2", tag = "key_btn_2", modifier = Modifier.weight(1f), onClick = { viewModel.onKeyPress("2") })
                        SimpleKey(text = "3", tag = "key_btn_3", modifier = Modifier.weight(1f), onClick = { viewModel.onKeyPress("3") })
                        SimpleKey(text = "+", tag = "key_btn_+", modifier = Modifier.weight(1f), isOperator = true, onClick = { viewModel.onKeyPress("+") })
                    }
                    // Row 5: History icon, 0, ., Equals FAB
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { showHistoryScreen = true }
                                .testTag("key_btn_history_toggle"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History",
                                tint = themeSecText,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        SimpleKey(text = "0", tag = "key_btn_0", modifier = Modifier.weight(1f), onClick = { viewModel.onKeyPress("0") })
                        SimpleKey(text = ".", tag = "key_btn_.", modifier = Modifier.weight(1f), onClick = { viewModel.onKeyPress(".") })
                        // Special FAB Style Equals inside a card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable { viewModel.onKeyPress("=") }
                                    .testTag("key_btn_="),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "=",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Right screen divider preview for tablets/landscape
            if (isLandscape || isTablet) {
                VerticalDivider(color = themeDivider, thickness = 1.dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(themeBgColor)
                ) {
                    SecureHistoryLockGuard(
                        isLocked = isHistoryLocked,
                        correctPinCode = historyPinCode,
                        isUnlocked = isHistoryUnlocked,
                        onUnlocked = { isHistoryUnlocked = true }
                    ) {
                        CalendarHistoryFullView(
                            activeHistory = activeHistory,
                            recycleBinHistory = recycleBinHistory,
                            viewModel = viewModel,
                            onClose = { }
                        )
                    }
                }
            }
        }

        // FULL SCREEN COMPREHENSIVE HISTORY SCREEN DRAWER OVERLAY
        AnimatedVisibility(
            visible = showHistoryScreen && !(isLandscape || isTablet),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(themeBgColor)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                SecureHistoryLockGuard(
                    isLocked = isHistoryLocked,
                    correctPinCode = historyPinCode,
                    isUnlocked = isHistoryUnlocked,
                    onUnlocked = { isHistoryUnlocked = true }
                ) {
                    CalendarHistoryFullView(
                        activeHistory = activeHistory,
                        recycleBinHistory = recycleBinHistory,
                        viewModel = viewModel,
                        onClose = { showHistoryScreen = false }
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleKey(
    text: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isOperator: Boolean = false,
    modifier: Modifier = Modifier,
    tag: String? = null,
    onClick: () -> Unit
) {
    val themeTextColor = if (isSystemInDarkTheme()) Color.White else Color(0xFF222222)
    val operatorColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .height(80.dp)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .testTag(tag ?: if (text != null) "key_btn_$text" else "key_btn_icon"),
        contentAlignment = Alignment.Center
    ) {
        if (text != null) {
            Text(
                text = text,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = if (isOperator) operatorColor else themeTextColor,
                style = MaterialTheme.typography.bodyLarge
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isOperator) operatorColor else themeTextColor.copy(alpha = 0.8f),
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

// HIGH FIDELITY HISTORY SYSTEM COMPONENT WITH POWERFUL ANALYTICS + SEARCH + RECYCLE BIN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarHistoryFullView(
    activeHistory: List<HistoryEntity>,
    recycleBinHistory: List<HistoryEntity>,
    viewModel: CalculatorViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterTab by remember { mutableStateOf("All") } // All, Recycling Bin, Favorites, Stats
    var filterChipTime by remember { mutableStateOf("Anytime") } // Anytime, Today, Yesterday, This Week, This Month

    // Multi-select tracking mapping
    val selectedIds = remember { mutableStateMapOf<Long, Boolean>() }
    var isSelectionModeActive by remember { mutableStateOf(false) }

    // Dropdowns and export formats
    var showExportMenu by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()
    val textBg = if (isDark) PremiumBackgroundDark else PremiumBackground
    val textCol = if (isDark) PremiumPrimaryTextDark else PremiumPrimaryText
    val secCol = if (isDark) PremiumSecondaryTextDark else PremiumSecondaryText
    val dividerCol = if (isDark) PremiumDividerDark else PremiumDivider

    // Filter computation logic
    val rawCalculations = if (selectedFilterTab == "Recycling Bin") recycleBinHistory else activeHistory
    val filteredHistory = remember(rawCalculations, searchQuery, selectedFilterTab, filterChipTime) {
        var processed = rawCalculations

        // Text Search
        if (searchQuery.isNotEmpty()) {
            processed = processed.filter {
                it.expression.contains(searchQuery, ignoreCase = true) ||
                it.result.contains(searchQuery, ignoreCase = true) ||
                it.tags.contains(searchQuery, ignoreCase = true) ||
                it.note.contains(searchQuery, ignoreCase = true)
            }
        }

        // Favorites tab filter
        if (selectedFilterTab == "Favorites") {
            processed = processed.filter { it.favorite }
        }

        // Time filtering
        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L
        val oneWeekMillis = 7 * oneDayMillis
        val thirtyDaysMillis = 30 * oneDayMillis

        processed = when (filterChipTime) {
            "Today" -> processed.filter { now - it.timestamp <= oneDayMillis }
            "Yesterday" -> processed.filter {
                val diff = now - it.timestamp
                diff in oneDayMillis..(2 * oneDayMillis)
            }
            "Last 7 Days" -> processed.filter { now - it.timestamp <= oneWeekMillis }
            "This Month" -> processed.filter { now - it.timestamp <= thirtyDaysMillis }
            else -> processed
        }

        processed
    }

    // Sort pinned favorites on top for the regular views
    val groupedHistory = remember(filteredHistory, selectedFilterTab) {
        // Group by Days: TODAY, YESTERDAY, OLDER
        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = formatter.format(Date(now))
        val yesterdayStr = formatter.format(Date(now - oneDayMillis))

        filteredHistory.groupBy {
            val itemStr = formatter.format(Date(it.timestamp))
            when (itemStr) {
                todayStr -> "TODAY"
                yesterdayStr -> "YESTERDAY"
                else -> "OLDER RECORDS"
            }
        }
    }

    // Statistical analysis computation
    val analyticsStats = remember(activeHistory) {
        val operatorCounts = mutableMapOf("+" to 0, "-" to 0, "×" to 0, "÷" to 0, "%" to 0)
        activeHistory.forEach {
            operatorCounts.keys.forEach { op ->
                if (it.expression.contains(op)) {
                    operatorCounts[op] = (operatorCounts[op] ?: 0) + 1
                }
            }
        }

        val dailyUsage = activeHistory.groupBy {
            SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(it.timestamp))
        }.mapValues { it.value.size }

        val pinsCount = activeHistory.count { it.favorite }

        Triple(operatorCounts, dailyUsage, pinsCount)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(textBg)
            .padding(16.dp)
    ) {
        // HEADER ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back back",
                        tint = PremiumPrimaryAccent
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "History",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = textCol
                    )
                )
            }

            // Global active history controls: Export dropdown, RecycleBin actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Clear state
                if (selectedFilterTab == "Recycling Bin") {
                    TextButton(
                        onClick = { viewModel.emptyRecycleBin() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Empty Bin", fontSize = 12.sp)
                    }
                } else {
                    IconButton(onClick = { showExportMenu = !showExportMenu }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share/Export Data",
                            tint = PremiumPrimaryAccent
                        )
                    }

                    IconButton(onClick = { viewModel.clearActiveHistory() }) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Move all active to trash bin",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // EXPORT FORMAT DIALOG
        if (showExportMenu) {
            DropdownMenu(
                expanded = showExportMenu,
                onDismissRequest = { showExportMenu = false },
                modifier = Modifier.background(textBg)
            ) {
                DropdownMenuItem(
                    text = { Text("Export as TXT String", color = textCol) },
                    onClick = {
                        showExportMenu = false
                        shareLogsAsText(context, filteredHistory, "text/plain")
                    }
                )
                DropdownMenuItem(
                    text = { Text("Export as CSV Dataset", color = textCol) },
                    onClick = {
                        showExportMenu = false
                        shareLogsAsText(context, filteredHistory, "text/csv")
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // TAB NAVIGATION: All, Favorites, Recycling Bin, Stats Panel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Favorites", "Recycling Bin", "Analytics").forEach { tab ->
                val isSelected = selectedFilterTab == tab
                Box(
                    modifier = Modifier
                        .scale(if (isSelected) 1.05f else 1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) PremiumPrimaryAccent else dividerCol)
                        .clickable {
                            selectedFilterTab = tab
                            selectedIds.clear()
                            isSelectionModeActive = false
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tab,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else textCol
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SEARCH INPUT BAR
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search logs, tags, or notes...", fontSize = 12.sp) },
            prefix = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp).padding(end = 4.dp),
                    tint = PremiumPrimaryAccent
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("history_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PremiumPrimaryAccent,
                unfocusedBorderColor = dividerCol,
                focusedContainerColor = dividerCol.copy(alpha = 0.3f),
                unfocusedContainerColor = dividerCol.copy(alpha = 0.2f),
                focusedTextColor = textCol,
                unfocusedTextColor = textCol
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // STATS AND ANALYTICS SHEET DRAW PANEL
        if (selectedFilterTab == "Analytics") {
            AnalyticsBoardPanel(analyticsStats, activeHistory.size, dividerCol, textCol)
        } else {
            // FILTER DAYS CHIPS ROW
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf("Anytime", "Today", "Yesterday", "Last 7 Days", "This Month")) { item ->
                    val isSelected = filterChipTime == item
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) PremiumPrimaryAccent.copy(alpha = 0.15f) else dividerCol.copy(alpha = 0.5f))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) PremiumPrimaryAccent else Color.Transparent,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { filterChipTime = item }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = item,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) PremiumPrimaryAccent else secCol
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // MULTI SELECTION HEADER CONTROLS BAR
            if (isSelectionModeActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(PremiumPrimaryAccent.copy(alpha = 0.08f))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedIds.filterValues { it }.size} selected",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PremiumPrimaryAccent
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                val list = selectedIds.filterValues { it }.keys.toList()
                                if (list.isNotEmpty()) {
                                    if (selectedFilterTab == "Recycling Bin") {
                                        viewModel.deletePermanentlyMultiple(list)
                                    } else {
                                        viewModel.moveMultipleToRecycleBin(list)
                                    }
                                }
                                selectedIds.clear()
                                isSelectionModeActive = false
                            }
                        ) {
                            Text("Delete Selected", fontSize = 11.sp, color = Color.Red)
                        }

                        TextButton(
                            onClick = {
                                isSelectionModeActive = false
                                selectedIds.clear()
                            }
                        ) {
                            Text("Cancel", fontSize = 11.sp, color = textCol)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // INFINITE EMPTY SCREEN OR ITEM LIST
            if (groupedHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = secCol.copy(alpha = 0.4f),
                            modifier = Modifier.size(50.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No calculations found matching filters.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = secCol
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    groupedHistory.forEach { (categoryDay, itemsList) ->
                        // Day Category Divider Heading
                        item {
                            Text(
                                text = categoryDay,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PremiumPrimaryAccent,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        // Render Cards for Calculations
                        items(itemsList) { item ->
                            val isSelected = selectedIds[item.id] == true
                            HistoryEntryCard(
                                item = item,
                                isSelected = isSelected,
                                isSelectionActive = isSelectionModeActive,
                                isRecycleBin = selectedFilterTab == "Recycling Bin",
                                textCol = textCol,
                                secCol = secCol,
                                dividerCol = dividerCol,
                                onClick = {
                                    if (isSelectionModeActive) {
                                        selectedIds[item.id] = !(selectedIds[item.id] ?: false)
                                    } else {
                                        viewModel.restoreEquation(item.expression, item.result)
                                        onClose()
                                    }
                                },
                                onLongClick = {
                                    isSelectionModeActive = true
                                    selectedIds[item.id] = true
                                },
                                onFavorite = { viewModel.toggleFavorite(item) },
                                onDelete = {
                                    if (selectedFilterTab == "Recycling Bin") {
                                        viewModel.deletePermanently(item.id)
                                    } else {
                                        viewModel.moveToRecycleBin(item.id)
                                    }
                                },
                                onRestore = { viewModel.restoreFromRecycleBin(item.id) },
                                onSaveNote = { newNote -> viewModel.updateNote(item, newNote) },
                                onSaveTags = { newTags -> viewModel.updateTags(item, newTags) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// SINGLE VISUAL CARD COMPONENT
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryEntryCard(
    item: HistoryEntity,
    isSelected: Boolean,
    isSelectionActive: Boolean,
    isRecycleBin: Boolean,
    textCol: Color,
    secCol: Color,
    dividerCol: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
    onSaveNote: (String) -> Unit,
    onSaveTags: (String) -> Unit
) {
    var isEditingDetails by remember { mutableStateOf(false) }
    var currentNoteText by remember { mutableStateOf(item.note) }
    var currentTagText by remember { mutableStateOf(item.tags) }

    val formattedTime = remember(item.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(item.timestamp))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) PremiumPrimaryAccent.copy(alpha = 0.08f) else dividerCol.copy(alpha = 0.25f))
            .border(
                width = 1.2.dp,
                color = if (isSelected) PremiumPrimaryAccent else if (item.favorite) PremiumPrimaryAccent.copy(alpha = 0.4f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // First Row: Meta Info (Time, Device, Edit detail action buttons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = secCol.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$formattedTime • ${item.deviceSource}",
                        fontSize = 10.sp,
                        color = secCol
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isRecycleBin) {
                        // Restore option
                        IconButton(onClick = onRestore, modifier = Modifier.size(20.dp)) {
                            Icon(
                                imageVector = Icons.Default.RestoreFromTrash,
                                contentDescription = "Restore Calculation",
                                tint = PremiumPrimaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        // Favorite toggle Star
                        IconButton(onClick = onFavorite, modifier = Modifier.size(20.dp)) {
                            Icon(
                                imageVector = if (item.favorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorite star",
                                tint = if (item.favorite) PremiumPrimaryAccent else secCol.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Pen to edit metadata note / tag inline
                        IconButton(
                            onClick = {
                                isEditingDetails = !isEditingDetails
                                if (!isEditingDetails) {
                                    onSaveNote(currentNoteText)
                                    onSaveTags(currentTagText)
                                }
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = if (isEditingDetails) Icons.Default.CheckCircle else Icons.Default.Edit,
                                contentDescription = "Edit calculation details",
                                tint = if (isEditingDetails) Color.Green else secCol.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Trash Bin Delete trigger
                    IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete item",
                            tint = Color.Red.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Second Row: Formula Equation
            Text(
                text = item.expression,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp
                ),
                color = textCol,
                modifier = Modifier.fillMaxWidth()
            )

            // Third Row: Evaluated Value Result matching exact formatting
            Text(
                text = "= ${item.result}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = PremiumPrimaryAccent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Dynamic inline tags / notes displays
            if (item.tags.isNotEmpty() || item.note.isNotEmpty() || isEditingDetails) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = dividerCol, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(6.dp))

                if (isEditingDetails) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = currentNoteText,
                            onValueChange = { currentNoteText = it },
                            placeholder = { Text("Write personal note...", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            textStyle = TextStyle(fontSize = 12.sp)
                        )

                        OutlinedTextField(
                            value = currentTagText,
                            onValueChange = { currentTagText = it },
                            placeholder = { Text("Tags (e.g., shopping, work)", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            textStyle = TextStyle(fontSize = 11.sp)
                        )
                    }
                } else {
                    if (item.note.isNotEmpty()) {
                        Text(
                            text = "Note: ${item.note}",
                            fontSize = 11.sp,
                            color = secCol.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Normal
                        )
                    }
                    if (item.tags.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            item.tags.split(",").forEach { tag ->
                                if (tag.trim().isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(PremiumPrimaryAccent.copy(alpha = 0.08f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "#${tag.trim()}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PremiumPrimaryAccent
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// PREMIUM STATS ANALYTICS COMPOSER COMPONENT
@Composable
fun AnalyticsBoardPanel(
    stats: Triple<Map<String, Int>, Map<String, Int>, Int>,
    totalCount: Int,
    cardBg: Color,
    textColor: Color
) {
    val (operatorUsage, dailyCounts, pinsCount) = stats

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Total Stats item
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardBg.copy(alpha = 0.4f))
                    .padding(12.dp)
            ) {
                Column {
                    Text("Total Cleared Logs", fontSize = 10.sp, color = PremiumPrimaryAccent)
                    Text("$totalCount", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColor)
                }
            }

            // Right Pin Stats item
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardBg.copy(alpha = 0.4f))
                    .padding(12.dp)
            ) {
                Column {
                    Text("Starred Formulas", fontSize = 10.sp, color = PremiumPrimaryAccent)
                    Text("$pinsCount", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColor)
                }
            }
        }

        // BAR CHART: Most Used Operators
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(cardBg.copy(alpha = 0.3f))
                .padding(14.dp)
        ) {
            Column {
                Text(
                    "Primary Operator Frequency Usage",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PremiumPrimaryAccent
                )
                Spacer(modifier = Modifier.height(10.dp))

                operatorUsage.forEach { (op, count) ->
                    val progress = if (totalCount > 0) count.toFloat() / totalCount else 0f
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(op, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.width(20.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(cardBg)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(if (progress > 1f) 1f else progress)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PremiumPrimaryAccent)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("$count times", fontSize = 11.sp, color = textColor)
                    }
                }
            }
        }

        // LINE GRAPH: Daily calculation metrics representation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(cardBg.copy(alpha = 0.3f))
                .padding(14.dp)
        ) {
            Column {
                Text(
                    "Temporal Daily Activities Map",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PremiumPrimaryAccent
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (dailyCounts.isEmpty()) {
                    Text("No usage recorded yet.", fontSize = 11.sp, color = textColor.copy(alpha = 0.5f))
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        dailyCounts.forEach { (day, ops) ->
                            val heightFraction = remember { (ops.toFloat() / 15f).coerceIn(0.1f, 1f) }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Text("$ops", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PremiumPrimaryAccent)
                                Box(
                                    modifier = Modifier
                                        .width(16.dp)
                                        .fillMaxHeight(heightFraction)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(PremiumPrimaryAccent)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(day, fontSize = 9.sp, color = textColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

// NATIVE SYSTEM TEXT RECIPES SHARE CONTROLLER
private fun shareLogsAsText(context: Context, history: List<HistoryEntity>, mimeType: String) {
    if (history.isEmpty()) return
    val stringBuilder = StringBuilder()

    if (mimeType == "text/csv") {
        stringBuilder.append("ID,Expression,Result,Timestamp,IsScientific,Tags,Notes,DeviceSource\n")
        history.forEach {
            stringBuilder.append("\"${it.id}\",\"${it.expression}\",\"${it.result}\",\"${it.timestamp}\",\"${it.isScientific}\",\"${it.tags}\",\"${it.note}\",\"${it.deviceSource}\"\n")
        }
    } else {
        stringBuilder.append("=== PREMIUM CALCULATOR CALCULATION REPORT ===\n\n")
        history.forEach {
            val formattedDate = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(Date(it.timestamp))
            stringBuilder.append("[$formattedDate] ${it.expression} = ${it.result}\n")
            if (it.note.isNotEmpty()) stringBuilder.append("👉 Note: ${it.note}\n")
            if (it.tags.isNotEmpty()) stringBuilder.append("🏷️ Tags: ${it.tags}\n")
            stringBuilder.append("--------------------------------------------------\n")
        }
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, stringBuilder.toString())
        putExtra(Intent.EXTRA_SUBJECT, "Cleared Math Calculation Sheets Data")
    }
    context.startActivity(Intent.createChooser(intent, "Export Calculation Sheets"))
}

// SECURE LOCK COMPONENT HELPER TO VERIFY RESTRICT VIEWING ACCESS
@Composable
fun SecureHistoryLockGuard(
    isLocked: Boolean,
    correctPinCode: String,
    onUnlocked: () -> Unit,
    isUnlocked: Boolean,
    fallbackContent: @Composable () -> Unit
) {
    if (isLocked && !isUnlocked) {
        var enteredPin by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "PIN Lock Secured",
                tint = PremiumPrimaryAccent,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Logs PIN Secured",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PremiumPrimaryText
            )
            Text(
                "Authentication is required to unlock calculation logs.",
                style = MaterialTheme.typography.bodySmall,
                color = PremiumSecondaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(
                                if (i < enteredPin.length) {
                                    if (pinError) Color.Red else PremiumPrimaryAccent
                                } else {
                                    PremiumDivider
                                }
                            )
                    )
                }
            }

            if (pinError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Incorrect PIN. Please try again.",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("C", "0", "⌫")
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                keys.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { digit ->
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(PremiumDivider)
                                    .clickable {
                                        pinError = false
                                        when (digit) {
                                            "C" -> enteredPin = ""
                                            "⌫" -> if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                            else -> {
                                                if (enteredPin.length < 4) {
                                                    enteredPin += digit
                                                    if (enteredPin.length == 4) {
                                                        if (enteredPin == correctPinCode) {
                                                            onUnlocked()
                                                        } else {
                                                            pinError = true
                                                            enteredPin = ""
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = digit,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PremiumPrimaryText
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        fallbackContent()
    }
}

