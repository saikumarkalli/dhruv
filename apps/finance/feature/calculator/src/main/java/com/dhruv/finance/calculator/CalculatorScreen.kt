package com.dhruv.finance.calculator

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.core.ui.components.Chip
import com.dhruv.core.ui.components.ModeChipRow
import com.dhruv.core.ui.components.NxInsetSurface
import com.dhruv.core.ui.components.PeriodChipRow
import com.dhruv.core.ui.components.Pill
import com.dhruv.core.ui.components.SearchField
import com.dhruv.core.ui.components.SectionLabel
import com.dhruv.core.ui.theme.*
import com.dhruv.finance.data.HistoryEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier,
    onOpenCurrency: () -> Unit = {},
    onOpenUnit: () -> Unit = {},
) {
    val context = LocalContext.current
    val inputState by viewModel.inputState.collectAsState()
    val result by viewModel.result.collectAsState()
    val activeHistory by viewModel.activeHistory.collectAsState()
    val clearedHistoryTime by viewModel.clearedHistoryTimestamp.collectAsState()
    val recycleBinHistory by viewModel.recycleBinHistory.collectAsState()
    val isDegree by viewModel.isDegree.collectAsState()
    val isHistoryLocked by viewModel.isHistoryLocked.collectAsState()
    val historyPinCode by viewModel.historyPinCode.collectAsState()
    val aiExplanationState by viewModel.aiExplanation.collectAsState()
    val formatLocale by viewModel.formatLocale.collectAsState()
    val isResultFinalised by viewModel.isResultFinalised.collectAsState()

    var showHistoryScreen by remember { mutableStateOf(false) }
    var isHistoryUnlocked by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    var isScientificMode by remember { mutableStateOf(false) }
    var isKeypadVisible by remember { mutableStateOf(true) }

    BackHandler(enabled = !isKeypadVisible) {
        isKeypadVisible = true
    }

    val nestedScrollConnection =
        remember {
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (isKeypadVisible && available.y > 15f) {
                        isKeypadVisible = false
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (!isKeypadVisible && available.y < -15f) {
                        isKeypadVisible = true
                    }
                    return Offset.Zero
                }
            }
        }

    val colors = LocalDhruvNextColors.current
    val themeBgColor = colors.bg
    val themeTextColor = colors.tx
    val themeSecText = colors.tx2
    val themeDivider = colors.line

    // DhruvNext hero type-scale ceiling (screen-size responsive: 32/38/46sp on small/phone/tablet).
    // The calculator's own content-length shrink-to-fit below (targetResultFontSize /
    // targetInputFontSize) still animates on top of this unchanged — only the ceiling those
    // animations shrink *from* now tracks screen size instead of a hardcoded 52f.
    val heroFontSize = DhruvNextType.hero.value

    val clipboardManager = LocalClipboardManager.current
    val hapticFeedback = LocalHapticFeedback.current

    val shakeOffsetX = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        viewModel.shakeEvent.collect {
            for (i in 0 until 3) {
                shakeOffsetX.animateTo(8f, animationSpec = tween(durationMillis = 30, easing = LinearEasing))
                shakeOffsetX.animateTo(-8f, animationSpec = tween(durationMillis = 30, easing = LinearEasing))
            }
            shakeOffsetX.animateTo(0f, animationSpec = tween(durationMillis = 30, easing = LinearEasing))
        }
    }

    LaunchedEffect(showHistoryScreen) {
        if (!showHistoryScreen) {
            isHistoryUnlocked = false
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.screenWidthDp >= 600

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(themeBgColor),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Main Column Area
            Column(
                modifier =
                    Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                        .animateContentSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 24.dp, vertical = 8.dp),
            ) {
                var dragAccumulatedX by remember { mutableStateOf(0f) }

                // Mode chip row (DhruvNext §6.3): Basic/Scientific are true local modes; Currency/Units
                // are pure navigation triggers (feature→feature is ArchUnit-forbidden, so this module
                // cannot render currency/unit content inline).
                val calcModeOptions =
                    listOf(
                        stringResource(R.string.calc_mode_basic),
                        stringResource(R.string.calc_mode_scientific),
                        stringResource(R.string.calc_mode_currency),
                        stringResource(R.string.calc_mode_units),
                    )
                ModeChipRow(
                    options = calcModeOptions,
                    selectedIndex = if (isScientificMode) 1 else 0,
                    onSelected = { index ->
                        when (index) {
                            0 -> isScientificMode = false
                            1 -> isScientificMode = true
                            2 -> onOpenCurrency()
                            3 -> onOpenUnit()
                        }
                    },
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp).testTag("calc_mode_chip_row"),
                )

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 12.dp)
                            .offset(x = shakeOffsetX.value.dp)
                            .clip(RoundedCornerShape(DhruvNextRadii.card))
                            .background(colors.surf2)
                            .combinedClickable(
                                onClick = { /* Do nothing */ },
                                onLongClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showContextMenu = true
                                },
                            ).pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragStart = { dragAccumulatedX = 0f },
                                    onHorizontalDrag = { _, dragAmount ->
                                        dragAccumulatedX += dragAmount
                                    },
                                    onDragEnd = {
                                        if (dragAccumulatedX < -150f) {
                                            viewModel.deleteLastToken()
                                        } else if (dragAccumulatedX < -30f) {
                                            viewModel.onKeyPress("⌫")
                                        }
                                    },
                                )
                            }.padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End,
                ) {
                    // Top row: History icon button, top-right of display. The old inline "Solve with
                    // AI" icon button now lives in the Explain/Tag/Save pill row below the result
                    // (§6.3) instead of duplicating the affordance here.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { showHistoryScreen = true },
                            modifier = Modifier.size(36.dp).testTag("key_btn_history_toggle"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History",
                                tint = themeSecText,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showContextMenu,
                        onDismissRequest = { showContextMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Copy Expression") },
                            onClick = {
                                showContextMenu = false
                                clipboardManager.setText(AnnotatedString(inputState.text))
                                Toast.makeText(context, "Expression copied", Toast.LENGTH_SHORT).show()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Copy Result") },
                            onClick = {
                                showContextMenu = false
                                val rawInputText = inputState.text
                                val cleanRes = if (result.isNotEmpty()) result.removePrefix("=").trim() else rawInputText
                                if (cleanRes.isNotEmpty() && !cleanRes.startsWith("Error")) {
                                    clipboardManager.setText(AnnotatedString(cleanRes))
                                    Toast.makeText(context, "Result copied", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "No result to copy", Toast.LENGTH_SHORT).show()
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Paste") },
                            onClick = {
                                showContextMenu = false
                                val pasted = clipboardManager.getText()?.text ?: ""
                                if (pasted.isNotEmpty()) {
                                    fun isValidExpression(text: String): Boolean {
                                        val cleaned =
                                            text
                                                .lowercase()
                                                .replace("sin", "")
                                                .replace("cos", "")
                                                .replace("tan", "")
                                                .replace("asin", "")
                                                .replace("acos", "")
                                                .replace("atan", "")
                                                .replace("log", "")
                                                .replace("ln", "")
                                                .replace("sqrt", "")
                                                .replace("pi", "")
                                                .replace("e", "")
                                        val allowedPattern = "^[0-9\\s\\.\\+\\-\\×\\÷\\*\\/\\%\\^\\!\\(\\)]*$"
                                        return cleaned.matches(Regex(allowedPattern))
                                    }
                                    if (isValidExpression(pasted)) {
                                        val normalized =
                                            pasted
                                                .replace("*", "×")
                                                .replace("/", "÷")
                                        val current = inputState.text
                                        val start = inputState.selection.min
                                        val end = inputState.selection.max
                                        val newText = current.substring(0, start) + normalized + current.substring(end)
                                        val newPos = start + normalized.length
                                        viewModel.updateInputState(TextFieldValue(newText, TextRange(newPos)))
                                        Toast.makeText(context, "Pasted successfully", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Invalid characters in clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                        )
                    }

                    // Content Area (History preview at top, divider, current calculation at bottom)
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .nestedScroll(nestedScrollConnection)
                                .pointerInput(isKeypadVisible) {
                                    if (isKeypadVisible) {
                                        detectVerticalDragGestures { _, dragAmount ->
                                            if (dragAmount > 15f) {
                                                isKeypadVisible = false
                                            }
                                        }
                                    }
                                },
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.End,
                    ) {
                        // ── RECENT HISTORY PREVIEW (top of card) ──
                        val recentHistory = activeHistory.filter { it.timestamp > clearedHistoryTime }
                        val displayHistory = if (isKeypadVisible) recentHistory.take(2) else recentHistory

                        LazyColumn(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.End,
                            reverseLayout = true,
                        ) {
                            items(displayHistory) { histEntry ->
                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewModel.restoreEquation(histEntry.expression, histEntry.result)
                                                isKeypadVisible = true
                                            }.padding(vertical = 4.dp),
                                    horizontalAlignment = Alignment.End,
                                ) {
                                    // Line 1: calculated value / expression (decreased text size, faded color)
                                    Text(
                                        text = histEntry.expression,
                                        style =
                                            TextStyle(
                                                fontSize = DhruvNextType.body,
                                                fontWeight = FontWeight.Normal,
                                                color = themeSecText.copy(alpha = 0.7f),
                                                textAlign = TextAlign.End,
                                            ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    // Line 2: result starts with '=' (decreased text size, faded color)
                                    Text(
                                        text = "= ${histEntry.result}",
                                        style =
                                            TextStyle(
                                                fontSize = DhruvNextType.title,
                                                fontWeight = FontWeight.Medium,
                                                color = themeSecText.copy(alpha = 0.85f),
                                                textAlign = TextAlign.End,
                                            ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }

                        // Subtle Divider
                        HorizontalDivider(
                            color = themeDivider,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )

                        // ── CURRENT CALCULATION (bottom of card) ──
                        if (isResultFinalised) {
                            // 1. Current expression with math signs (small size)
                            val lastExpr by viewModel.lastExpression.collectAsState()
                            Text(
                                text = lastExpr,
                                style =
                                    TextStyle(
                                        fontSize = DhruvNextType.title,
                                        fontWeight = FontWeight.Normal,
                                        color = themeSecText,
                                        textAlign = TextAlign.End,
                                    ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            // 2. Final Result starting with '=' (large size)
                            val resultScrollState = rememberScrollState()
                            val rawInputText = inputState.text
                            val cleanRes = if (result.isNotEmpty()) result.removePrefix("=").trim() else rawInputText
                            val displayResult =
                                if (cleanRes.startsWith("Error")) {
                                    cleanRes
                                } else {
                                    "= ${viewModel.formatLocaleSeparator(cleanRes, formatLocale)}"
                                }

                            val targetResultFontSize =
                                (heroFontSize * (7f / maxOf(7f, cleanRes.length.toFloat()))).coerceIn(22f, heroFontSize)

                            val animatedResultFontSize by animateFloatAsState(
                                targetValue = targetResultFontSize,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                                label = "resultFontSize",
                            )

                            Text(
                                text = displayResult,
                                style =
                                    MaterialTheme.typography.headlineLarge.copy(
                                        fontSize = animatedResultFontSize.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                color = themeTextColor,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(resultScrollState)
                                        .combinedClickable(
                                            onClick = { /* nothing */ },
                                            onLongClick = {
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                clipboardManager.setText(AnnotatedString(cleanRes))
                                                Toast.makeText(context, "Copied: $cleanRes", Toast.LENGTH_SHORT).show()
                                            },
                                        ).testTag("calc_result_field"),
                            )
                        } else {
                            // 1. BasicTextField showing inputState (large size)
                            // Preserves the original 44/52 ratio between the live-input ceiling and the
                            // finalized-result hero ceiling above — both now derive from heroFontSize.
                            val inputHeroCeiling = heroFontSize * (44f / 52f)
                            val targetInputFontSize =
                                (inputHeroCeiling * (11f / maxOf(11f, inputState.text.length.toFloat()))).coerceIn(18f, inputHeroCeiling)
                            val animatedInputFontSize by animateFloatAsState(
                                targetValue = targetInputFontSize,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                label = "inputFontSize",
                            )
                            val inputFontSize = animatedInputFontSize.sp

                            BasicTextField(
                                value = inputState,
                                onValueChange = {
                                    viewModel.updateInputState(it)
                                },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .testTag("calc_input_field"),
                                readOnly = true,
                                textStyle =
                                    TextStyle(
                                        fontSize = inputFontSize,
                                        fontWeight = FontWeight.Medium,
                                        color = themeTextColor,
                                        textAlign = TextAlign.End,
                                        lineHeight = (inputFontSize.value * 1.25f).sp,
                                    ),
                                singleLine = true,
                                cursorBrush = SolidColor(colors.acc),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.CenterEnd,
                                    ) {
                                        if (inputState.text.isEmpty()) {
                                            Text(
                                                text = "0",
                                                style =
                                                    TextStyle(
                                                        fontSize = inputFontSize,
                                                        fontWeight = FontWeight.Medium,
                                                        color = themeTextColor.copy(alpha = 0.3f),
                                                        textAlign = TextAlign.End,
                                                    ),
                                            )
                                        }
                                        innerTextField()
                                    }
                                },
                            )

                            // 2. Result Preview starting with '=' (small size)
                            if (result.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                val resultScrollState = rememberScrollState()
                                val cleanRes = result.removePrefix("=").trim()
                                val displayResult =
                                    if (cleanRes.startsWith("Error")) {
                                        cleanRes
                                    } else {
                                        "= ${viewModel.formatLocaleSeparator(cleanRes, formatLocale)}"
                                    }

                                // Preserves the original 22/52 ratio to the hero ceiling for this smaller
                                // typing-preview result.
                                val previewHeroCeiling = heroFontSize * (22f / 52f)
                                val targetResultFontSize =
                                    (previewHeroCeiling * (10f / maxOf(10f, cleanRes.length.toFloat())))
                                        .coerceIn(12f, previewHeroCeiling)

                                val animatedResultFontSize by animateFloatAsState(
                                    targetValue = targetResultFontSize,
                                    label = "resultFontSize",
                                )

                                Text(
                                    text = displayResult,
                                    style =
                                        MaterialTheme.typography.headlineLarge.copy(
                                            fontSize = animatedResultFontSize.sp,
                                            fontWeight = FontWeight.Normal,
                                        ),
                                    color = themeSecText.copy(alpha = 0.65f),
                                    textAlign = TextAlign.End,
                                    maxLines = 1,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(resultScrollState),
                                )
                            } else {
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                    }

                    // ── EXPLAIN / TAG / SAVE PILL ROW (§6.3) ──
                    // Explain = the existing "Solve with AI" affordance, restyled as a pill (same
                    // viewModel.solveCurrentInput() call + AiExplanationState bottom sheet as before).
                    // Tag has no backing ViewModel action on the live calculation yet, so it renders
                    // as a real but disabled pill rather than faking persistence. Save is wired to the
                    // closest honest existing action: starring the just-computed history entry via
                    // viewModel.toggleFavorite (identical to the star toggle already in HistoryEntryCard).
                    val latestHistoryEntry = activeHistory.firstOrNull()
                    val canExplain = inputState.text.isNotEmpty()
                    val canSave = isResultFinalised && latestHistoryEntry != null
                    val isLatestSaved = latestHistoryEntry?.favorite == true

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Pill(
                            label = stringResource(R.string.calc_pill_explain),
                            leadingIcon = Icons.Default.AutoAwesome,
                            onClick = if (canExplain) ({ viewModel.solveCurrentInput() }) else null,
                            modifier =
                                Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .alpha(if (canExplain) 1f else 0.45f)
                                    .testTag("calc_pill_explain"),
                        )
                        Pill(
                            label = stringResource(R.string.calc_pill_tag),
                            leadingIcon = Icons.AutoMirrored.Filled.Label,
                            onClick = null,
                            modifier =
                                Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .alpha(0.45f)
                                    .testTag("calc_pill_tag"),
                        )
                        Pill(
                            label =
                                stringResource(
                                    if (isLatestSaved) R.string.calc_pill_saved else R.string.calc_pill_save,
                                ),
                            leadingIcon = if (isLatestSaved) Icons.Default.Star else Icons.Default.StarBorder,
                            selected = isLatestSaved,
                            onClick = if (canSave) ({ viewModel.toggleFavorite(latestHistoryEntry!!) }) else null,
                            modifier =
                                Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .alpha(if (canSave) 1f else 0.45f)
                                    .testTag("calc_pill_save"),
                        )
                    }
                }

                // Subtle visual horizontal separator
                HorizontalDivider(
                    color = themeDivider,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 4.dp),
                )

                // ── KEYPAD AREA (Feature 6: background follows system dark/light) ──
                AnimatedVisibility(
                    visible = isKeypadVisible,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .widthIn(max = 480.dp)
                            .align(Alignment.CenterHorizontally)
                            .then(if (isKeypadVisible) Modifier.weight(1.2f) else Modifier),
                    enter = slideInVertically(initialOffsetY = { it }) + expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(colors.bg)
                                .padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        // ── Scientific expansion rows ──────────────────────────
                        val sciKeyHeight = if (isScientificMode) 56.dp else 0.dp
                        AnimatedVisibility(
                            visible = isScientificMode,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut(),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                // Sci row A: DEG/RAD toggle, sin, cos, tan, ^
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                                ) {
                                    // DEG / RAD toggle
                                    Box(
                                        modifier =
                                            Modifier
                                                .weight(1f)
                                                .height(sciKeyHeight)
                                                .background(colors.surf)
                                                .clickable { viewModel.toggleAngleUnit() }
                                                .testTag("key_btn_deg_rad"),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = if (isDegree) "DEG" else "RAD",
                                            fontSize = DhruvNextKeypad.caption,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.acc,
                                        )
                                    }
                                    SimpleKey(
                                        text = "sin",
                                        tag = "key_btn_sin",
                                        fontSize = DhruvNextKeypad.function,
                                        modifier =
                                            Modifier.weight(
                                                1f,
                                            ),
                                        keyHeight = sciKeyHeight,
                                        isOperator = true,
                                        onClick = {
                                            viewModel.onKeyPress("sin")
                                        },
                                    )
                                    SimpleKey(
                                        text = "cos",
                                        tag = "key_btn_cos",
                                        fontSize = DhruvNextKeypad.function,
                                        modifier =
                                            Modifier.weight(
                                                1f,
                                            ),
                                        keyHeight = sciKeyHeight,
                                        isOperator = true,
                                        onClick = {
                                            viewModel.onKeyPress("cos")
                                        },
                                    )
                                    SimpleKey(
                                        text = "tan",
                                        tag = "key_btn_tan",
                                        fontSize = DhruvNextKeypad.function,
                                        modifier =
                                            Modifier.weight(
                                                1f,
                                            ),
                                        keyHeight = sciKeyHeight,
                                        isOperator = true,
                                        onClick = {
                                            viewModel.onKeyPress("tan")
                                        },
                                    )
                                    SimpleKey(
                                        text = "xʸ",
                                        tag = "key_btn_power",
                                        fontSize = DhruvNextKeypad.function,
                                        modifier =
                                            Modifier.weight(
                                                1f,
                                            ),
                                        keyHeight = sciKeyHeight,
                                        isOperator = true,
                                        onClick = {
                                            viewModel.onKeyPress("^")
                                        },
                                    )
                                }
                                // Sci row B: (, ), log, ln, √
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                                ) {
                                    SimpleKey(
                                        text = "(",
                                        tag = "key_btn_open_bracket",
                                        fontSize = DhruvNextKeypad.function,
                                        modifier =
                                            Modifier.weight(
                                                1f,
                                            ),
                                        keyHeight = sciKeyHeight,
                                        onClick = {
                                            viewModel.onKeyPress("(")
                                        },
                                    )
                                    SimpleKey(
                                        text = ")",
                                        tag = "key_btn_close_bracket",
                                        fontSize = DhruvNextKeypad.function,
                                        modifier =
                                            Modifier.weight(
                                                1f,
                                            ),
                                        keyHeight = sciKeyHeight,
                                        onClick = {
                                            viewModel.onKeyPress(")")
                                        },
                                    )
                                    SimpleKey(
                                        text = "log",
                                        tag = "key_btn_log",
                                        fontSize = DhruvNextKeypad.function,
                                        modifier =
                                            Modifier.weight(
                                                1f,
                                            ),
                                        keyHeight = sciKeyHeight,
                                        isOperator = true,
                                        onClick = {
                                            viewModel.onKeyPress("log")
                                        },
                                    )
                                    SimpleKey(
                                        text = "ln",
                                        tag = "key_btn_ln",
                                        fontSize = DhruvNextKeypad.function,
                                        modifier =
                                            Modifier.weight(
                                                1f,
                                            ),
                                        keyHeight = sciKeyHeight,
                                        isOperator = true,
                                        onClick = {
                                            viewModel.onKeyPress("ln")
                                        },
                                    )
                                    SimpleKey(
                                        text = "√",
                                        tag = "key_btn_sqrt",
                                        fontSize = DhruvNextKeypad.function,
                                        modifier =
                                            Modifier.weight(
                                                1f,
                                            ),
                                        keyHeight = sciKeyHeight,
                                        isOperator = true,
                                        onClick = {
                                            viewModel.onKeyPress("sqrt")
                                        },
                                    )
                                }
                            }
                        }

                        // ── Standard rows ──────────────────────────────────────
                        val stdH = androidx.compose.ui.unit.Dp.Unspecified // Use unspecified so keys expand to fill row weight

                        // Row 1: C, ⌫, %, ÷  (Feature 3: C and ⌫ share operator accent colour)
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            val clearText = if (inputState.text.isNotEmpty()) "C" else "AC"
                            SimpleKey(
                                text = clearText,
                                tag = "key_btn_C",
                                modifier =
                                    Modifier.weight(
                                        1f,
                                    ),
                                keyHeight = stdH,
                                isOperator = true,
                                onClick = {
                                    if (clearText == "AC") {
                                        viewModel.clearCalcScreenHistory()
                                    }
                                    viewModel.onKeyPress(clearText)
                                },
                            )
                            SimpleKey(
                                icon = Icons.AutoMirrored.Filled.Backspace,
                                tag = "key_btn_⌫",
                                modifier =
                                    Modifier.weight(
                                        1f,
                                    ),
                                keyHeight = stdH,
                                isOperator = true,
                                onClick = {
                                    viewModel.onKeyPress("⌫")
                                },
                            )
                            SimpleKey(
                                text = "%",
                                tag = "key_btn_%",
                                modifier =
                                    Modifier.weight(
                                        1f,
                                    ),
                                keyHeight = stdH,
                                fontSize = DhruvNextKeypad.operator,
                                isOperator = true,
                                onClick = {
                                    viewModel.onKeyPress("%")
                                },
                            )
                            SimpleKey(
                                text = "÷",
                                tag = "key_btn_÷",
                                modifier =
                                    Modifier.weight(
                                        1f,
                                    ),
                                keyHeight = stdH,
                                fontSize = DhruvNextKeypad.operator,
                                isOperator = true,
                                onClick = {
                                    viewModel.onKeyPress("÷")
                                },
                            )
                        }
                        // Row 2: 7, 8, 9, ×
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            SimpleKey(
                                text = "7",
                                tag = "key_btn_7",
                                modifier = Modifier.weight(1f),
                                keyHeight = stdH,
                                onClick = { viewModel.onKeyPress("7") },
                            )
                            SimpleKey(
                                text = "8",
                                tag = "key_btn_8",
                                modifier = Modifier.weight(1f),
                                keyHeight = stdH,
                                onClick = { viewModel.onKeyPress("8") },
                            )
                            SimpleKey(
                                text = "9",
                                tag = "key_btn_9",
                                modifier = Modifier.weight(1f),
                                keyHeight = stdH,
                                onClick = { viewModel.onKeyPress("9") },
                            )
                            SimpleKey(
                                text = "×",
                                tag = "key_btn_×",
                                modifier =
                                    Modifier.weight(
                                        1f,
                                    ),
                                keyHeight = stdH,
                                fontSize = DhruvNextKeypad.operator,
                                isOperator = true,
                                onClick = {
                                    viewModel.onKeyPress("×")
                                },
                            )
                        }
                        // Row 3: 4, 5, 6, −
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            SimpleKey(
                                text = "4",
                                tag = "key_btn_4",
                                modifier = Modifier.weight(1f),
                                keyHeight = stdH,
                                onClick = { viewModel.onKeyPress("4") },
                            )
                            SimpleKey(
                                text = "5",
                                tag = "key_btn_5",
                                modifier = Modifier.weight(1f),
                                keyHeight = stdH,
                                onClick = { viewModel.onKeyPress("5") },
                            )
                            SimpleKey(
                                text = "6",
                                tag = "key_btn_6",
                                modifier = Modifier.weight(1f),
                                keyHeight = stdH,
                                onClick = { viewModel.onKeyPress("6") },
                            )
                            SimpleKey(
                                text = "−",
                                tag = "key_btn_-",
                                modifier =
                                    Modifier.weight(
                                        1f,
                                    ),
                                keyHeight = stdH,
                                fontSize = DhruvNextKeypad.operator,
                                isOperator = true,
                                onClick = {
                                    viewModel.onKeyPress("-")
                                },
                            )
                        }
                        // Row 4: 1, 2, 3, +
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            SimpleKey(
                                text = "1",
                                tag = "key_btn_1",
                                modifier = Modifier.weight(1f),
                                keyHeight = stdH,
                                onClick = { viewModel.onKeyPress("1") },
                            )
                            SimpleKey(
                                text = "2",
                                tag = "key_btn_2",
                                modifier = Modifier.weight(1f),
                                keyHeight = stdH,
                                onClick = { viewModel.onKeyPress("2") },
                            )
                            SimpleKey(
                                text = "3",
                                tag = "key_btn_3",
                                modifier = Modifier.weight(1f),
                                keyHeight = stdH,
                                onClick = { viewModel.onKeyPress("3") },
                            )
                            SimpleKey(
                                text = "+",
                                tag = "key_btn_+",
                                modifier =
                                    Modifier.weight(
                                        1f,
                                    ),
                                keyHeight = stdH,
                                fontSize = DhruvNextKeypad.operator,
                                isOperator = true,
                                onClick = {
                                    viewModel.onKeyPress("+")
                                },
                            )
                        }
                        // Row 5: 0 (double-width — replaces the old dedicated Scientific-toggle key,
                        // now the mode chip row at the top of the screen), ., =
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SimpleKey(
                                text = "0",
                                tag = "key_btn_0",
                                modifier = Modifier.weight(2f),
                                keyHeight = stdH,
                                onClick = { viewModel.onKeyPress("0") },
                                onLongClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onKeyPress("000")
                                },
                            )
                            SimpleKey(
                                text = ".",
                                tag = "key_btn_.",
                                modifier = Modifier.weight(1f),
                                keyHeight = stdH,
                                onClick = { viewModel.onKeyPress(".") },
                            )

                            // Equals button
                            val equalInteractionSource = remember { MutableInteractionSource() }
                            val equalIsPressed by equalInteractionSource.collectIsPressedAsState()
                            val equalGlowAlpha by animateFloatAsState(
                                targetValue = if (equalIsPressed) 0.25f else 0f,
                                animationSpec = tween(durationMillis = if (equalIsPressed) 50 else 200),
                                label = "equalGlowAlpha",
                            )
                            Box(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(colors.surf),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .padding(4.dp)
                                            .clip(CircleShape)
                                            .background(colors.acc)
                                            .combinedClickable(
                                                interactionSource = equalInteractionSource,
                                                indication = null,
                                                onClick = {
                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    viewModel.onKeyPress("=")
                                                },
                                                onLongClick = {
                                                    val res = result.removePrefix("=").trim()
                                                    if (res.isNotEmpty() && !res.startsWith("Error")) {
                                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        clipboardManager.setText(AnnotatedString(res))
                                                        Toast.makeText(context, "Result copied", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                            ).drawWithContent {
                                                drawContent()
                                                if (equalGlowAlpha > 0f) {
                                                    val glowBrush =
                                                        Brush.radialGradient(
                                                            colors =
                                                                listOf(
                                                                    colors.acc.copy(alpha = equalGlowAlpha),
                                                                    Color.Transparent,
                                                                ),
                                                            radius = size.minDimension * 0.8f,
                                                        )
                                                    drawRect(brush = glowBrush)
                                                }
                                            }.testTag("key_btn_="),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "=",
                                        color = colors.onAcc,
                                        fontSize = DhruvNextKeypad.operator,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Right screen divider preview for tablets/landscape
            if (isLandscape || isTablet) {
                VerticalDivider(color = themeDivider, thickness = 1.dp)
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(themeBgColor),
                ) {
                    SecureHistoryLockGuard(
                        isLocked = isHistoryLocked,
                        correctPinCode = historyPinCode,
                        isUnlocked = isHistoryUnlocked,
                        onUnlocked = { isHistoryUnlocked = true },
                    ) {
                        CalendarHistoryFullView(
                            activeHistory = activeHistory,
                            recycleBinHistory = recycleBinHistory,
                            viewModel = viewModel,
                            onClose = { },
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
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(themeBgColor)
                        .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                SecureHistoryLockGuard(
                    isLocked = isHistoryLocked,
                    correctPinCode = historyPinCode,
                    isUnlocked = isHistoryUnlocked,
                    onUnlocked = { isHistoryUnlocked = true },
                ) {
                    CalendarHistoryFullView(
                        activeHistory = activeHistory,
                        recycleBinHistory = recycleBinHistory,
                        viewModel = viewModel,
                        onClose = { showHistoryScreen = false },
                    )
                }
            }
        }

        if (aiExplanationState !is AiExplanationState.Idle) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.clearAiExplanation() },
                containerColor = colors.surf,
                shape = RoundedCornerShape(topStart = DhruvNextRadii.card, topEnd = DhruvNextRadii.card),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = colors.acc,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI Solver",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = themeTextColor,
                            )
                        }
                        IconButton(onClick = { viewModel.clearAiExplanation() }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when (val state = aiExplanationState) {
                        is AiExplanationState.Loading -> {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = colors.acc)
                            }
                        }
                        is AiExplanationState.Success -> {
                            // Answer + short note: first non-blank line is the answer, the rest is context.
                            val lines =
                                state.explanation
                                    .split("\n")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                            val answer = lines.firstOrNull().orEmpty()
                            val note = lines.drop(1).joinToString(" ")
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = answer,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = themeTextColor,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                if (note.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = note,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = themeSecText,
                                        modifier = Modifier.fillMaxWidth(),
                                        lineHeight = 20.sp,
                                    )
                                }
                            }
                        }
                        is AiExplanationState.Error -> {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.neg,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        else -> {}
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SimpleKey(
    text: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    badgeText: String? = null,
    isOperator: Boolean = false,
    modifier: Modifier = Modifier,
    tag: String? = null,
    keyHeight: androidx.compose.ui.unit.Dp = 72.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = DhruvNextKeypad.digit,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val colors = LocalDhruvNextColors.current
    val themeTextColor = colors.tx
    val operatorColor = colors.acc
    val hapticFeedback = LocalHapticFeedback.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Feature 4: Circular centered ripple effect (200 ms fade-in/out)
    val rippleProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec =
            tween(
                durationMillis = if (isPressed) 80 else 200,
                easing = FastOutSlowInEasing,
            ),
        label = "rippleProgress",
    )
    // Pick the ripple colour: operator keys glow with the accent, numeric keys with a neutral tx glow.
    val rippleBaseColor = if (isOperator) operatorColor else themeTextColor

    Box(
        modifier =
            modifier
                .then(if (keyHeight == androidx.compose.ui.unit.Dp.Unspecified) Modifier.fillMaxHeight() else Modifier.height(keyHeight))
                .background(colors.surf)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    },
                    onLongClick =
                        onLongClick?.let {
                            {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                it()
                            }
                        },
                ).drawWithContent {
                    drawContent()
                    // Circular light ripple centered on the key
                    if (rippleProgress > 0f) {
                        val maxRadius = size.minDimension * 0.9f
                        val currentRadius = maxRadius * rippleProgress
                        val alpha = (0.28f * (1f - rippleProgress * 0.5f)).coerceAtLeast(0f)
                        val brush =
                            Brush.radialGradient(
                                colors =
                                    listOf(
                                        rippleBaseColor.copy(alpha = alpha),
                                        rippleBaseColor.copy(alpha = alpha * 0.4f),
                                        Color.Transparent,
                                    ),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = currentRadius,
                            )
                        drawCircle(
                            brush = brush,
                            radius = currentRadius,
                            center = Offset(size.width / 2f, size.height / 2f),
                        )
                    }
                }.testTag(tag ?: if (text != null) "key_btn_$text" else "key_btn_icon"),
        contentAlignment = Alignment.Center,
    ) {
        if (text != null) {
            Text(
                text = text,
                fontSize = fontSize,
                // Operators read as the primary "action" keys, so give them a touch more weight
                // than the numerals for a clearer visual hierarchy.
                fontWeight = if (isOperator) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isOperator) operatorColor else themeTextColor,
                style = MaterialTheme.typography.bodyLarge,
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isOperator) operatorColor else themeTextColor.copy(alpha = 0.8f),
                modifier = Modifier.size(26.dp),
            )
        }

        if (badgeText != null) {
            Text(
                text = badgeText,
                fontSize = DhruvNextKeypad.caption,
                fontWeight = FontWeight.Bold,
                color = colors.acc,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 24.dp),
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
    onClose: () -> Unit,
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

    val colors = LocalDhruvNextColors.current
    val textBg = colors.bg
    val textCol = colors.tx
    val secCol = colors.tx2
    val dividerCol = colors.line

    // Filter computation logic
    val rawCalculations = if (selectedFilterTab == "Recycling Bin") recycleBinHistory else activeHistory
    val filteredHistory =
        remember(rawCalculations, searchQuery, selectedFilterTab, filterChipTime) {
            var processed = rawCalculations

            // Text Search
            if (searchQuery.isNotEmpty()) {
                processed =
                    processed.filter {
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

            processed =
                when (filterChipTime) {
                    "Today" -> processed.filter { now - it.timestamp <= oneDayMillis }
                    "Yesterday" ->
                        processed.filter {
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
    val groupedHistory =
        remember(filteredHistory, selectedFilterTab) {
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
    val analyticsStats =
        remember(activeHistory) {
            val operatorCounts = mutableMapOf("+" to 0, "-" to 0, "×" to 0, "÷" to 0, "%" to 0)
            activeHistory.forEach {
                operatorCounts.keys.forEach { op ->
                    if (it.expression.contains(op)) {
                        operatorCounts[op] = (operatorCounts[op] ?: 0) + 1
                    }
                }
            }

            val dailyUsage =
                activeHistory
                    .groupBy {
                        SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(it.timestamp))
                    }.mapValues { it.value.size }

            val pinsCount = activeHistory.count { it.favorite }

            Triple(operatorCounts, dailyUsage, pinsCount)
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(textBg)
                .padding(16.dp),
    ) {
        // HEADER ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.acc,
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "History",
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = textCol,
                        ),
                )
            }

            // Global active history controls: Export dropdown, RecycleBin actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Clear state
                if (selectedFilterTab == "Recycling Bin") {
                    TextButton(
                        onClick = { viewModel.emptyRecycleBin() },
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.neg),
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Empty Bin", fontSize = DhruvNextType.meta)
                    }
                } else {
                    IconButton(onClick = { showExportMenu = !showExportMenu }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share/Export Data",
                            tint = colors.acc,
                        )
                    }

                    IconButton(onClick = { viewModel.clearActiveHistory() }) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Move all active to trash bin",
                            tint = colors.neg,
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
                modifier = Modifier.background(textBg),
            ) {
                DropdownMenuItem(
                    text = { Text("Export as TXT String", color = textCol) },
                    onClick = {
                        showExportMenu = false
                        shareLogsAsText(context, filteredHistory, "text/plain")
                    },
                )
                DropdownMenuItem(
                    text = { Text("Export as CSV Dataset", color = textCol) },
                    onClick = {
                        showExportMenu = false
                        shareLogsAsText(context, filteredHistory, "text/csv")
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // TAB NAVIGATION: All, Favorites, Recycling Bin, Stats Panel
        val historyTabs = listOf("All", "Favorites", "Recycling Bin", "Analytics")
        ModeChipRow(
            options = historyTabs,
            selectedIndex = historyTabs.indexOf(selectedFilterTab).coerceAtLeast(0),
            onSelected = { index ->
                selectedFilterTab = historyTabs[index]
                selectedIds.clear()
                isSelectionModeActive = false
            },
            modifier = Modifier.testTag("history_tab_row"),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // SEARCH INPUT BAR
        SearchField(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = "Search logs, tags, or notes...",
            modifier = Modifier.testTag("history_search_input"),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // STATS AND ANALYTICS SHEET DRAW PANEL
        if (selectedFilterTab == "Analytics") {
            AnalyticsBoardPanel(analyticsStats, activeHistory.size)
        } else {
            // FILTER DAYS CHIPS ROW (reuses the same period-selector shape as Insights' month picker)
            val timeFilters = listOf("Anytime", "Today", "Yesterday", "Last 7 Days", "This Month")
            PeriodChipRow(
                options = timeFilters,
                selectedIndex = timeFilters.indexOf(filterChipTime).coerceAtLeast(0),
                onSelected = { index -> filterChipTime = timeFilters[index] },
                modifier = Modifier.testTag("history_time_filter_row"),
            )

            Spacer(modifier = Modifier.height(10.dp))

            // MULTI SELECTION HEADER CONTROLS BAR
            if (isSelectionModeActive) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.accSoft)
                            .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${selectedIds.filterValues { it }.size} selected",
                        fontSize = DhruvNextType.meta,
                        fontWeight = FontWeight.Bold,
                        color = colors.acc,
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
                            },
                        ) {
                            Text("Delete Selected", fontSize = DhruvNextType.meta, color = colors.neg)
                        }

                        TextButton(
                            onClick = {
                                isSelectionModeActive = false
                                selectedIds.clear()
                            },
                        ) {
                            Text("Cancel", fontSize = DhruvNextType.meta, color = textCol)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // INFINITE EMPTY SCREEN OR ITEM LIST
            if (groupedHistory.isEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = secCol.copy(alpha = 0.4f),
                            modifier = Modifier.size(50.dp),
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No calculations found matching filters.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = secCol,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    groupedHistory.forEach { (categoryDay, itemsList) ->
                        // Day Category Divider Heading
                        item {
                            SectionLabel(
                                text = categoryDay,
                                modifier = Modifier.padding(top = 8.dp),
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
                                onSaveTags = { newTags -> viewModel.updateTags(item, newTags) },
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
    onSaveTags: (String) -> Unit,
) {
    val colors = LocalDhruvNextColors.current
    var isEditingDetails by remember { mutableStateOf(false) }
    var currentNoteText by remember { mutableStateOf(item.note) }
    var currentTagText by remember { mutableStateOf(item.tags) }

    val formattedTime =
        remember(item.timestamp) {
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(item.timestamp))
        }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DhruvNextRadii.card))
                .background(if (isSelected) colors.accSoft else colors.surf)
                .border(
                    width = 1.2.dp,
                    color =
                        if (isSelected) {
                            colors.acc
                        } else if (item.favorite) {
                            colors.accLine
                        } else {
                            Color.Transparent
                        },
                    shape = RoundedCornerShape(DhruvNextRadii.card),
                ).combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ).padding(14.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // First Row: Meta Info (Time, Device, Edit detail action buttons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = secCol.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$formattedTime • ${item.deviceSource}",
                        fontSize = DhruvNextType.meta,
                        color = secCol,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isRecycleBin) {
                        // Restore option
                        IconButton(onClick = onRestore, modifier = Modifier.size(20.dp)) {
                            Icon(
                                imageVector = Icons.Default.RestoreFromTrash,
                                contentDescription = "Restore Calculation",
                                tint = colors.acc,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    } else {
                        // Favorite toggle Star
                        IconButton(onClick = onFavorite, modifier = Modifier.size(20.dp)) {
                            Icon(
                                imageVector = if (item.favorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorite star",
                                tint = if (item.favorite) colors.acc else secCol.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp),
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
                            modifier = Modifier.size(20.dp),
                        ) {
                            Icon(
                                imageVector = if (isEditingDetails) Icons.Default.CheckCircle else Icons.Default.Edit,
                                contentDescription = "Edit calculation details",
                                tint = if (isEditingDetails) colors.pos else secCol.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    // Trash Bin Delete trigger
                    IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete item",
                            tint = colors.neg.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Second Row: Formula Equation
            Text(
                text = item.expression,
                style =
                    MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = DhruvNextType.title,
                    ),
                color = textCol,
                modifier = Modifier.fillMaxWidth(),
            )

            // Third Row: Evaluated Value Result matching exact formatting
            Text(
                text = "= ${item.result}",
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = DhruvNextType.title,
                        color = colors.acc,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )

            // Dynamic inline tags / notes displays
            if (item.tags.isNotEmpty() || item.note.isNotEmpty() || isEditingDetails) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = dividerCol, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(6.dp))

                if (isEditingDetails) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        OutlinedTextField(
                            value = currentNoteText,
                            onValueChange = { currentNoteText = it },
                            placeholder = { Text("Write personal note...", fontSize = DhruvNextType.meta) },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            textStyle = TextStyle(fontSize = DhruvNextType.meta),
                        )

                        OutlinedTextField(
                            value = currentTagText,
                            onValueChange = { currentTagText = it },
                            placeholder = { Text("Tags (e.g., shopping, work)", fontSize = DhruvNextType.meta) },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            textStyle = TextStyle(fontSize = DhruvNextType.meta),
                        )
                    }
                } else {
                    if (item.note.isNotEmpty()) {
                        Text(
                            text = "Note: ${item.note}",
                            fontSize = DhruvNextType.meta,
                            color = secCol.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Normal,
                        )
                    }
                    if (item.tags.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            item.tags.split(",").forEach { tag ->
                                if (tag.trim().isNotEmpty()) {
                                    Chip(label = "#${tag.trim()}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// STATS ANALYTICS PANEL — DhruvNext tokens, tonal NxInsetSurface tiles + SectionLabel headers.
@Composable
fun AnalyticsBoardPanel(
    stats: Triple<Map<String, Int>, Map<String, Int>, Int>,
    totalCount: Int,
) {
    val colors = LocalDhruvNextColors.current
    val (operatorUsage, dailyCounts, pinsCount) = stats

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Left Total Stats item
            NxInsetSurface(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    SectionLabel(text = "Total Cleared Logs")
                    Text("$totalCount", fontSize = DhruvNextType.hero, fontWeight = FontWeight.Bold, color = colors.tx)
                }
            }

            // Right Pin Stats item
            NxInsetSurface(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    SectionLabel(text = "Starred Formulas")
                    Text("$pinsCount", fontSize = DhruvNextType.hero, fontWeight = FontWeight.Bold, color = colors.tx)
                }
            }
        }

        // BAR CHART: Most Used Operators
        NxInsetSurface(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                SectionLabel(text = "Primary Operator Frequency Usage")
                Spacer(modifier = Modifier.height(10.dp))

                operatorUsage.forEach { (op, count) ->
                    val progress = if (totalCount > 0) count.toFloat() / totalCount else 0f
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            op,
                            fontSize = DhruvNextType.meta,
                            fontWeight = FontWeight.Bold,
                            color = colors.tx,
                            modifier = Modifier.width(20.dp),
                        )
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colors.line2),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(if (progress > 1f) 1f else progress)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(colors.acc),
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("$count times", fontSize = DhruvNextType.meta, color = colors.tx)
                    }
                }
            }
        }

        // LINE GRAPH: Daily calculation metrics representation
        NxInsetSurface(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                SectionLabel(text = "Temporal Daily Activities Map")
                Spacer(modifier = Modifier.height(12.dp))

                if (dailyCounts.isEmpty()) {
                    Text("No usage recorded yet.", fontSize = DhruvNextType.meta, color = colors.tx.copy(alpha = 0.5f))
                } else {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        dailyCounts.forEach { (day, ops) ->
                            val heightFraction = remember { (ops.toFloat() / 15f).coerceIn(0.1f, 1f) }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                            ) {
                                Text("$ops", fontSize = DhruvNextType.meta, fontWeight = FontWeight.Bold, color = colors.acc)
                                Box(
                                    modifier =
                                        Modifier
                                            .width(16.dp)
                                            .fillMaxHeight(heightFraction)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(colors.acc),
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(day, fontSize = DhruvNextType.meta, color = colors.tx)
                            }
                        }
                    }
                }
            }
        }
    }
}

// NATIVE SYSTEM TEXT RECIPES SHARE CONTROLLER
private fun shareLogsAsText(
    context: Context,
    history: List<HistoryEntity>,
    mimeType: String,
) {
    if (history.isEmpty()) return
    val stringBuilder = StringBuilder()

    if (mimeType == "text/csv") {
        stringBuilder.append("ID,Expression,Result,Timestamp,IsScientific,Tags,Notes,DeviceSource\n")
        history.forEach {
            stringBuilder.append(
                "\"${it.id}\",\"${it.expression}\",\"${it.result}\",\"${it.timestamp}\",\"${it.isScientific}\",\"${it.tags}\",\"${it.note}\",\"${it.deviceSource}\"\n",
            )
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

    val intent =
        Intent(Intent.ACTION_SEND).apply {
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
    fallbackContent: @Composable () -> Unit,
) {
    if (isLocked && !isUnlocked) {
        val colors = LocalDhruvNextColors.current
        var enteredPin by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf(false) }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "PIN Lock Secured",
                tint = colors.acc,
                modifier = Modifier.size(56.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Logs PIN Secured",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.tx,
            )
            Text(
                "Authentication is required to unlock calculation logs.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.tx2,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (i in 0 until 4) {
                    Box(
                        modifier =
                            Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i < enteredPin.length) {
                                        if (pinError) colors.neg else colors.acc
                                    } else {
                                        colors.line2
                                    },
                                ),
                    )
                }
            }

            if (pinError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Incorrect PIN. Please try again.",
                    color = colors.neg,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            val keys =
                listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "⌫"),
                )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                keys.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { digit ->
                            Box(
                                modifier =
                                    Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(colors.surf2)
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
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = digit,
                                    fontSize = DhruvNextKeypad.digit,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.tx,
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
