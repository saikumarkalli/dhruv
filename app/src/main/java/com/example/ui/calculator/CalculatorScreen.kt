package com.example.ui.calculator

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HistoryEntity
import com.example.ui.theme.*
import androidx.compose.foundation.border

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val input by viewModel.input.collectAsState()
    val result by viewModel.result.collectAsState()
    val history by viewModel.history.collectAsState()
    val isDegree by viewModel.isDegree.collectAsState()
    val isHistoryLocked by viewModel.isHistoryLocked.collectAsState()
    val historyPinCode by viewModel.historyPinCode.collectAsState()

    var showScientific by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var isHistoryUnlocked by remember { mutableStateOf(false) }

    LaunchedEffect(showHistorySheet) {
        if (!showHistorySheet) {
            isHistoryUnlocked = false
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.screenWidthDp >= 600

    Box(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Left main area: screen display and key grid
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
                    .padding(top = 8.dp)
            ) {
                // Header panel: Title, Rad/Deg state
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Calculator Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Calculator",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = ResponsiveApp.typography.titleLarge),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // History peek button
                        IconButton(
                            onClick = { showHistorySheet = true },
                            modifier = Modifier.testTag("history_toggle_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History Logs",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Calculations Display Box (Scrolled and automatically scaled layout)
                // Gesture navigation: Swiping LEFT opens History.
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, dragAmount ->
                                if (dragAmount < -20f && !showHistorySheet && !(isTablet || isLandscape)) {
                                    showHistorySheet = true
                                }
                                change.consume()
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.End
                    ) {
                        // Swipe Left instruction
                        Text(
                            text = "Swipe Left for history scroll",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )

                        // PAST TAPE RECORDS PREVIEW (top 3, 4, or more depending on screen size)
                        if (history.isNotEmpty() && (!isHistoryLocked || isHistoryUnlocked)) {
                            val targetCount = if (isTablet || isLandscape) 5 else 3
                            val recentLogs = history.take(targetCount).reversed()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                recentLogs.forEach { log ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.restoreEquation(log.expression, log.result) }
                                            .padding(vertical = 4.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = log.expression,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "= ${log.result}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Formula text input
                        Text(
                            text = input.ifEmpty { "0" },
                            style = if (input.length > 15) {
                                MaterialTheme.typography.headlineMedium.copy(fontSize = ResponsiveApp.typography.headlineMedium)
                            } else {
                                MaterialTheme.typography.displayMedium.copy(fontSize = ResponsiveApp.typography.displayMedium)
                            },
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.End,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("calc_input_field")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Evaluated Results
                        Text(
                            text = result,
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = ResponsiveApp.typography.headlineLarge),
                            color = if (result == "Error") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("calc_result_field")
                        )
                    }
                }

                // Keyboard grid controller
                // Gesture control: Drag up to reveal scientific keys, drag down to hide them.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (MaterialTheme.colorScheme.background != Color.White) Color(0xFF212429) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                        )
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { change, dragAmount ->
                                if (dragAmount < -15f && !showScientific) {
                                    showScientific = true
                                } else if (dragAmount > 15f && showScientific) {
                                    showScientific = false
                                }
                                change.consume()
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header of keyboard: reveals expandable slider drawer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showScientific) "SWIPE DOWN FOR BASIC" else "SWIPE UP / TAP FOR SCIENTIFIC",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable { showScientific = !showScientific }
                                .padding(vertical = 4.dp)
                        )

                        IconButton(
                            onClick = { showScientific = !showScientific },
                            modifier = Modifier.size(24.dp).testTag("scientific_toggle_arrow")
                        ) {
                            Icon(
                                imageVector = if (showScientific) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = "Toggle Scientific Keyboard",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Key grids
                    AnimatedVisibility(
                        visible = showScientific,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        ScientificKeyGrid(onKeyPress = { viewModel.onKeyPress(it) })
                    }

                    BasicKeyGrid(onKeyPress = { viewModel.onKeyPress(it) })
                }
            }

            // Right partition structure: responsive desktop/tablet layout showing history side-by-side
            if (isTablet || isLandscape) {
                Surface(
                    modifier = Modifier
                        .weight(0.7f)
                        .fillMaxHeight()
                        .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    SecureHistoryLockGuard(
                        isLocked = isHistoryLocked,
                        correctPinCode = historyPinCode,
                        isUnlocked = isHistoryUnlocked,
                        onUnlocked = { isHistoryUnlocked = true }
                    ) {
                        DateWiseHistoryView(
                            history = history,
                            onSelectRecord = { record ->
                                viewModel.restoreEquation(record.expression, record.result)
                            },
                            onDeleteRecord = { record ->
                                viewModel.deleteHistoryId(record.id)
                            },
                            onClearAll = {
                                viewModel.clearHistory()
                            },
                            modifier = Modifier.fillMaxSize().padding(16.dp)
                        )
                    }
                }
            }
        }

        // Full Screen Sliding overlay modal-drawer-like container for mobile display with Gesture Controls
        AnimatedVisibility(
            visible = showHistorySheet && !(isTablet || isLandscape),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            // Swipe right gesture to dismiss
                            if (dragAmount > 20f) {
                                showHistorySheet = false
                            }
                            change.consume()
                        }
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Gesture pull close bar
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(60.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { change, dragAmount ->
                                    if (dragAmount > 15f) {
                                        showHistorySheet = false
                                    }
                                    change.consume()
                                }
                            }
                            .clickable { showHistorySheet = false }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showHistorySheet = false }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Close history",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Calculation History (Swipe pull down to dismiss)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SecureHistoryLockGuard(
                        isLocked = isHistoryLocked,
                        correctPinCode = historyPinCode,
                        isUnlocked = isHistoryUnlocked,
                        onUnlocked = { isHistoryUnlocked = true }
                    ) {
                        DateWiseHistoryView(
                            history = history,
                            onSelectRecord = { record ->
                                viewModel.restoreEquation(record.expression, record.result)
                                showHistorySheet = false
                            },
                            onDeleteRecord = { record ->
                                viewModel.deleteHistoryId(record.id)
                            },
                            onClearAll = {
                                viewModel.clearHistory()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

private fun formatHistoryHeader(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val sdf = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault())
    val todaySdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val itemDateStr = todaySdf.format(date)
    val todayDateStr = todaySdf.format(java.util.Date())
    val yesterdayDateStr = todaySdf.format(java.util.Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))
    
    return when (itemDateStr) {
        todayDateStr -> "Today"
        yesterdayDateStr -> "Yesterday"
        else -> sdf.format(date)
    }
}

@Composable
fun DateWiseHistoryView(
    history: List<HistoryEntity>,
    onSelectRecord: (HistoryEntity) -> Unit,
    onDeleteRecord: (HistoryEntity) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val groupedHistory = remember(history) {
        history.groupBy { record ->
            formatHistoryHeader(record.timestamp)
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Calculation Records",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (history.isNotEmpty()) {
                TextButton(
                    onClick = onClearAll,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("clear_all_history_button")
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All")
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        if (history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No calculations performed yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groupedHistory.forEach { (dateHeader, records) ->
                    item {
                        Text(
                            text = dateHeader,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp)
                        )
                    }
                    items(records, key = { it.id }) { record ->
                        HistoryRowItem(
                            record = record,
                            onTap = { onSelectRecord(record) },
                            onDelete = { onDeleteRecord(record) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BasicKeyGrid(onKeyPress: (String) -> Unit) {
    val keys = listOf(
        listOf("AC", "⌫", "±", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "(", "=")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { char ->
                    Box(modifier = Modifier.weight(1f)) {
                        CalcKeyButton(
                            text = char,
                            onClick = { onKeyPress(char) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScientificKeyGrid(onKeyPress: (String) -> Unit) {
    val keys = listOf(
        listOf("sin", "cos", "tan", "^"),
        listOf("asin", "acos", "atan", "!"),
        listOf("log", "ln", "sqrt", "%"),
        listOf("π", "e", ")", "C")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(8.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { char ->
                    Box(modifier = Modifier.weight(1f)) {
                        CalcKeyButton(
                            text = char,
                            isScientific = true,
                            onClick = { onKeyPress(char) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalcKeyButton(
    text: String,
    isScientific: Boolean = false,
    onClick: () -> Unit
) {
    val isPrimaryAction = text == "="
    val isClearKey = text == "AC" || text == "⌫" || text == "C"
    val isModifier = text == "%" || text == "(" || text == ")" || text == "±" || text == "()"
    val isOperator = text == "÷" || text == "×" || text == "-" || text == "+"

    val isDarkTheme = MaterialTheme.colorScheme.background != Color.White

    val containerColor = when {
        isPrimaryAction -> if (isDarkTheme) SophEqualsBg else MaterialTheme.colorScheme.primary
        isOperator -> if (isDarkTheme) SophOpKeyBg else MaterialTheme.colorScheme.primaryContainer
        isClearKey || isModifier -> if (isDarkTheme) SophModKeyBg else MaterialTheme.colorScheme.secondaryContainer
        isScientific -> if (isDarkTheme) SophScienceKeyBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        else -> if (isDarkTheme) SophNumKeyBg else MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when {
        isPrimaryAction -> if (isDarkTheme) SophEqualsText else MaterialTheme.colorScheme.onPrimary
        isOperator -> if (isDarkTheme) SophOpKeyText else MaterialTheme.colorScheme.onPrimaryContainer
        isClearKey || isModifier -> if (isDarkTheme) SophModKeyText else MaterialTheme.colorScheme.onSecondaryContainer
        isScientific -> if (isDarkTheme) SophScienceKeyText else MaterialTheme.colorScheme.primary
        else -> if (isDarkTheme) SophNumKeyText else MaterialTheme.colorScheme.onSurface
    }

    val borderStroke = if (isScientific && isDarkTheme) {
        androidx.compose.foundation.BorderStroke(1.dp, SophScienceKeyBorder)
    } else {
        null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ResponsiveApp.dimens.calculatorKeyHeight)
            .clip(RoundedCornerShape(16.dp))
            .then(if (borderStroke != null) Modifier.border(borderStroke, RoundedCornerShape(16.dp)) else Modifier)
            .background(containerColor)
            .clickable { onClick() }
            .testTag("key_btn_$text"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = ResponsiveApp.typography.titleMedium),
            fontWeight = FontWeight.Bold,
            color = contentColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun HistoryRowItem(
    record: HistoryEntity,
    onTap: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .testTag("history_item_${record.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.expression,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "= ${record.result}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp).testTag("delete_history_btn_${record.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete Log Record",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

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
                contentDescription = "PIN Lock",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Logs PIN Secured",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Authentication is required to unlock calculation logs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
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
                                    if (pinError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                    )
                }
            }

            if (pinError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Incorrect PIN. Please try again.",
                    color = MaterialTheme.colorScheme.error,
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

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                keys.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { digit ->
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
