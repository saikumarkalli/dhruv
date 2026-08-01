package com.dhruv.finance.currency

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhruv.core.ui.components.DhruvModalSheet
import com.dhruv.core.ui.components.ListGroup
import com.dhruv.core.ui.components.ListGroupRow
import com.dhruv.core.ui.components.OfflineBanner
import com.dhruv.core.ui.components.Pill
import com.dhruv.core.ui.components.RetryErrorCard
import com.dhruv.core.ui.components.SearchField
import com.dhruv.core.ui.components.SectionLabel
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Currency converter screen extracted from the monolithic ConverterScreen.
 * Depends solely on [CurrencyViewModel] which is wired via Koin.
 *
 * Note: the top-level back/sync toolbar is expected to be provided by the
 * Converter hub in the app shell (or a wrapper composable) that navigates
 * between currency and unit sub-screens. This composable owns only the
 * conversion UI body.
 *
 * DhruvNext §6.6 shape: a single `surf`+`line` converter card holding a From row and a To row
 * separated by a `line2` hairline, with a 40dp accent swap FAB overlapping both rows on the right;
 * quick-amount [Pill] chips; a two-sided rate/staleness caption; a "your currencies" [ListGroup]
 * preview (per-row unit-rate subline + an "Edit" action); and an [OfflineBanner] disclosure when
 * running on cached rates.
 */
@Composable
fun CurrencyScreen(
    viewModel: CurrencyViewModel,
    modifier: Modifier = Modifier,
) {
    CurrencyConverterContent(viewModel = viewModel, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyConverterContent(
    viewModel: CurrencyViewModel,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current

    val currencyInput by viewModel.currencyInput.collectAsState()
    val currencyFrom by viewModel.currencyFrom.collectAsState()
    val currencyTo by viewModel.currencyTo.collectAsState()
    val currencyResult by viewModel.currencyResult.collectAsState()
    val currencyStatus by viewModel.currencyStatus.collectAsState()
    val lastUpdatedTime by viewModel.lastUpdatedTime.collectAsState()
    val ratesMap by viewModel.ratesMap.collectAsState()

    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        focusManager.clearFocus()
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DhruvNextSpacing.screenGutter, vertical = DhruvNextSpacing.screenGutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Width-capped, centered content column — tablet responsiveness (DhruvNext §6.6 requirement);
        // the outer Column above stays full-width so the scroll gesture still spans the whole screen.
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = CURRENCY_CONTENT_MAX_WIDTH)
                    .align(Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
        ) {
            when (val status = currencyStatus) {
                is CurrencyViewModel.CurrencyStatus.Loading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = colors.acc, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Refreshing currency rates…", color = colors.tx2, fontSize = DhruvNextType.body)
                    }
                }
                is CurrencyViewModel.CurrencyStatus.Error -> {
                    RetryErrorCard(message = status.message, onRetry = { viewModel.syncCurrencyRates() })
                }
                is CurrencyViewModel.CurrencyStatus.Success -> Unit
            }

            // Single converter card: From row + To row split by a hairline, with the swap FAB
            // overlapping both rows on the right (DhruvNext §6.6 card-structure delta).
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 1.dp, shape = RoundedCornerShape(DhruvNextRadii.card), clip = false)
                            .clip(RoundedCornerShape(DhruvNextRadii.card))
                            .background(colors.surf)
                            .border(width = 1.dp, color = colors.line, shape = RoundedCornerShape(DhruvNextRadii.card))
                            .padding(CONVERTER_CARD_PADDING),
                ) {
                    CurrencyConverterRow(
                        code = currencyFrom,
                        onSelectorClick = { showFromPicker = true },
                        selectorTestTag = "currency_from_btn",
                    ) {
                        CurrencyAmountField(
                            value = currencyInput,
                            onValueChange = { viewModel.setCurrencyInput(it) },
                            modifier = Modifier.fillMaxWidth().testTag("currency_input_field"),
                        )
                    }

                    HorizontalDivider(color = colors.line2, thickness = 1.dp)

                    CurrencyConverterRow(
                        code = currencyTo,
                        onSelectorClick = { showToPicker = true },
                        selectorTestTag = "currency_to_btn",
                    ) {
                        Text(
                            text = currencyResult.ifBlank { "0" },
                            color = colors.acc,
                            fontSize = AMOUNT_FONT_SIZE,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End,
                            style = TextStyle(fontFeatureSettings = "tnum", letterSpacing = AMOUNT_LETTER_SPACING),
                            modifier = Modifier.fillMaxWidth().testTag("currency_output_val"),
                        )
                    }
                }

                FilledIconButton(
                    onClick = {
                        val from = currencyFrom
                        val to = currencyTo
                        viewModel.setCurrencyFrom(to)
                        viewModel.setCurrencyTo(from)
                    },
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = SWAP_FAB_END_INSET)
                            .shadow(elevation = 4.dp, shape = CircleShape)
                            .size(SWAP_FAB_SIZE)
                            .testTag("currency_swap_btn"),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.acc, contentColor = colors.onAcc),
                ) {
                    Icon(imageVector = Icons.Default.SwapVert, contentDescription = "Swap currencies")
                }
            }

            // Quick-amount chips.
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(QUICK_AMOUNTS) { (label, rawValue) ->
                    Pill(
                        label = label,
                        selected = currencyInput == rawValue,
                        onClick = { viewModel.setCurrencyInput(rawValue) },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    )
                }
            }

            // Rate + staleness caption row: rate on the left, a clock glyph + freshness on the right.
            val rateText =
                remember(currencyFrom, currencyTo, ratesMap) {
                    buildRateText(currencyFrom, currencyTo, ratesMap)
                }
            val freshnessText =
                remember(lastUpdatedTime, currencyStatus) {
                    buildFreshnessText(lastUpdatedTime, currencyStatus)
                }
            val isStale by viewModel.isStale.collectAsState()
            val isLoading = currencyStatus is CurrencyViewModel.CurrencyStatus.Loading
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = rateText ?: "Rate unavailable", color = colors.tx2, fontSize = RATE_CAPTION_FONT_SIZE)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (isStale) colors.acc else colors.tx3,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = freshnessText,
                        color = if (isStale) colors.acc else colors.tx3,
                        fontSize = FRESHNESS_CAPTION_FONT_SIZE,
                    )
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = colors.acc,
                            strokeWidth = 1.5.dp,
                        )
                    } else {
                        IconButton(
                            onClick = { viewModel.syncCurrencyRates() },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh rates",
                                tint = colors.tx3,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            // "<amount> <currency> in your currencies" preview list.
            val otherCurrencies =
                remember(currencyFrom, currencyTo) {
                    PREFERRED_OTHER_CURRENCIES
                        .filter { it in viewModel.availableCurrencies && it != currencyFrom && it != currencyTo }
                        .take(MAX_OTHER_CURRENCIES)
                }
            if (ratesMap.isNotEmpty() && otherCurrencies.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val formattedInputAmount =
                        remember(currencyInput) {
                            currencyInput.toDoubleOrNull()?.let(::formatConvertedAmount) ?: currencyInput
                        }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SectionLabel(text = "$formattedInputAmount $currencyFrom in your currencies")
                        Text(
                            text = "Edit",
                            color = colors.acc,
                            fontSize = EDIT_ACTION_FONT_SIZE,
                            fontWeight = FontWeight.Bold,
                            modifier =
                                Modifier
                                    .clickable(onClickLabel = "Edit currency list") {
                                        // No persisted "favourite currencies" list exists yet (out
                                        // of scope for DhruvNext §6.6 / Task 3) — rendered so the
                                        // action reads correctly, wired to a no-op rather than
                                        // faking a working edit flow.
                                    }
                                    .testTag("currency_others_edit_action"),
                        )
                    }
                    ListGroup(
                        rows =
                            otherCurrencies.map { code ->
                                {
                                    ListGroupRow(
                                        title = "$code · ${currencyDisplayName(code)}",
                                        subtitle = buildUnitRateCaption(code, currencyFrom, ratesMap),
                                        showChevron = false,
                                        trailing = {
                                            val converted =
                                                convertViaUsdBase(currencyInput.toDoubleOrNull(), currencyFrom, code, ratesMap)
                                            Text(
                                                text = converted?.let { formatConvertedAmount(it) } ?: "—",
                                                color = colors.tx,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = PREVIEW_TRAILING_FONT_SIZE,
                                            )
                                        },
                                    )
                                }
                            },
                    )
                }
            }

            // Offline / cached-rates disclosure, bottom of screen.
            val isOfflineCached = (currencyStatus as? CurrencyViewModel.CurrencyStatus.Success)?.isOffline == true
            if (isOfflineCached) {
                OfflineBanner(message = "Showing cached exchange rates. Connect to the internet to refresh.")
            }
        }
    }

    if (showFromPicker) {
        CurrencyPickerSheet(
            currencies = viewModel.availableCurrencies,
            selectedCode = currencyFrom,
            onSelect = { viewModel.setCurrencyFrom(it) },
            onDismiss = { showFromPicker = false },
        )
    }
    if (showToPicker) {
        CurrencyPickerSheet(
            currencies = viewModel.availableCurrencies,
            selectedCode = currencyTo,
            onSelect = { viewModel.setCurrencyTo(it) },
            onDismiss = { showToPicker = false },
        )
    }
}

/**
 * One row inside the merged converter card: a [CurrencySelectorRow] on the left, arbitrary
 * [amountContent] (editable field or static result) right-aligned in the remaining space. The
 * extra end padding reserves room for the swap FAB that overlaps the card on the right so the
 * amount text never renders underneath it.
 */
@Composable
private fun CurrencyConverterRow(
    code: String,
    onSelectorClick: () -> Unit,
    selectorTestTag: String,
    modifier: Modifier = Modifier,
    amountContent: @Composable () -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = ROW_HORIZONTAL_PADDING, vertical = ROW_VERTICAL_PADDING)
                .padding(end = SWAP_FAB_ROW_GUTTER),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CurrencySelectorRow(
            code = code,
            onClick = onSelectorClick,
            modifier = Modifier.testTag(selectorTestTag),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            amountContent()
        }
    }
}

/**
 * Borderless, right-aligned hero-styled numeric input — the merged card's editable "From" amount
 * (DhruvNext §6.6's amount-display delta). Keeps the numeric keyboard and editability of the old
 * [OutlinedTextField] but with no visible border/underline, matching the mock's large plain digits.
 */
@Composable
private fun CurrencyAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle =
            TextStyle(
                color = colors.tx,
                fontSize = AMOUNT_FONT_SIZE,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                letterSpacing = AMOUNT_LETTER_SPACING,
                fontFeatureSettings = "tnum",
            ),
        cursorBrush = SolidColor(colors.acc),
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                if (value.isEmpty()) {
                    Text(
                        text = "0",
                        color = colors.tx3,
                        fontSize = AMOUNT_FONT_SIZE,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                innerTextField()
            }
        },
    )
}

/** A small rounded-square tile showing a currency's code — the "initials tile" leading every
 * currency selector row (DhruvNext §6.6). */
@Composable
private fun CurrencyInitialsTile(
    code: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    Box(
        modifier =
            modifier
                .size(size)
                .clip(RoundedCornerShape(DhruvNextRadii.innerTile))
                .background(colors.surf2),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = code, color = colors.tx2, fontSize = TILE_CODE_FONT_SIZE, fontWeight = FontWeight.Bold)
    }
}

/** The tappable currency selector atop each converter row: initials tile + code + name +
 * `expand_more` (DhruvNext §6.6's currency-selector-row delta). */
@Composable
private fun CurrencySelectorRow(
    code: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(DhruvNextRadii.innerTile))
                .clickable(onClickLabel = "Change currency", onClick = onClick)
                .defaultMinSize(minHeight = 48.dp)
                .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CurrencyInitialsTile(code = code, size = SELECTOR_TILE_SIZE)
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = code, color = colors.tx, fontSize = SELECTOR_CODE_FONT_SIZE, fontWeight = FontWeight.Bold)
            Text(text = currencyDisplayName(code), color = colors.tx3, fontSize = SELECTOR_NAME_FONT_SIZE)
        }
        Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = null,
            tint = colors.tx2,
            modifier = Modifier.padding(start = 2.dp).size(20.dp),
        )
    }
}

/** Searchable currency picker — [DhruvModalSheet] + [SearchField] + [ListGroup]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyPickerSheet(
    currencies: List<String>,
    selectedCode: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalDhruvNextColors.current
    var query by remember { mutableStateOf("") }
    val filtered =
        remember(query, currencies) {
            if (query.isBlank()) currencies else currencies.filter { it.contains(query, ignoreCase = true) }
        }

    DhruvModalSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DhruvNextSpacing.screenGutter)
                    .padding(bottom = DhruvNextSpacing.screenGutter)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
        ) {
            Text(text = "Select currency", color = colors.tx, fontSize = DhruvNextType.title, fontWeight = FontWeight.Bold)
            SearchField(query = query, onQueryChange = { query = it }, placeholder = "Search currency")
            ListGroup(
                rows =
                    filtered.map { code ->
                        {
                            ListGroupRow(
                                title = code,
                                subtitle = currencyDisplayName(code),
                                showChevron = false,
                                onClick = {
                                    onSelect(code)
                                    onDismiss()
                                },
                                trailing = {
                                    if (code == selectedCode) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = colors.acc)
                                    }
                                },
                            )
                        }
                    },
            )
        }
    }
}

/** Fixed preset amounts (label, raw VM input string) — DhruvNext §6.6's quick-amount chip row. */
private val QUICK_AMOUNTS =
    listOf(
        "1,000" to "1000",
        "10,000" to "10000",
        "85,000" to "85000",
        "1,00,000" to "100000",
    )

/** Preferred display order for the "in your currencies" preview list. */
private val PREFERRED_OTHER_CURRENCIES =
    listOf("EUR", "GBP", "JPY", "INR", "AUD", "CAD", "CHF", "CNY", "SGD", "NZD", "ZAR", "BRL")

private const val MAX_OTHER_CURRENCIES = 4

/**
 * Static ISO-4217 code -> short display-name lookup for the selector subtitle and the "in your
 * currencies" list (DhruvNext §6.6). Covers every code in [CurrencyViewModel.availableCurrencies] /
 * [PREFERRED_OTHER_CURRENCIES]; an unmapped code (a future addition to either list) falls back to
 * the code itself via [currencyDisplayName] rather than crashing or rendering blank.
 */
private val CURRENCY_DISPLAY_NAMES: Map<String, String> =
    mapOf(
        "USD" to "Dollar",
        "EUR" to "Euro",
        "GBP" to "Pound",
        "INR" to "Rupee",
        "JPY" to "Yen",
        "CAD" to "Canadian Dollar",
        "AUD" to "Australian Dollar",
        "CNY" to "Yuan",
        "CHF" to "Swiss Franc",
        "NZD" to "NZ Dollar",
        "ZAR" to "Rand",
        "SGD" to "Singapore Dollar",
        "BRL" to "Real",
    )

/** [code]'s short display name, or [code] itself if unmapped — never blank, never throws. */
internal fun currencyDisplayName(code: String): String = CURRENCY_DISPLAY_NAMES[code] ?: code

/**
 * Same via-USD conversion [CurrencyViewModel.recalculateCurrency] already performs, applied here
 * purely for the supplementary "in your currencies" preview — the authoritative [currencyResult]
 * for the selected To-currency still comes entirely from the ViewModel, untouched.
 */
private fun convertViaUsdBase(
    amount: Double?,
    fromCode: String,
    toCode: String,
    rates: Map<String, Double>,
): Double? {
    val fromRate = rates[fromCode]?.takeIf { it != 0.0 }
    val toRate = rates[toCode]
    return if (amount != null && fromRate != null && toRate != null) {
        (amount / fromRate) * toRate
    } else {
        null
    }
}

private fun formatConvertedAmount(value: Double): String {
    val df = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US))
    return df.format(value)
}

/**
 * "1 USD = 0.9163 EUR" style headline rate text — DhruvNext §6.6's rate/staleness caption row,
 * left side. Returns `null` when either rate is missing so the caller can show a plain
 * "unavailable" placeholder instead of a misleading number.
 */
internal fun buildRateText(
    fromCode: String,
    toCode: String,
    rates: Map<String, Double>,
): String? {
    val fromRate = rates[fromCode]
    val toRate = rates[toCode]
    if (fromRate == null || toRate == null || fromRate == 0.0) return null
    val perUnit = toRate / fromRate
    val df = DecimalFormat("#,##0.####", DecimalFormatSymbols(Locale.US))
    return "1 $fromCode = ${df.format(perUnit)} $toCode"
}

/**
 * "Updated 12min ago · cached" style freshness text — DhruvNext §6.6's rate/staleness caption row,
 * right side (paired with a `schedule` clock glyph by the caller).
 */
internal fun buildFreshnessText(
    lastUpdatedTime: Long?,
    status: CurrencyViewModel.CurrencyStatus,
): String {
    val timeStr =
        lastUpdatedTime?.let {
            val diffMs = System.currentTimeMillis() - it
            val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs) % 60
            when {
                hours > 0 -> "${hours}hr ${minutes}min ago"
                minutes > 0 -> "${minutes}min ago"
                else -> "just now"
            }
        } ?: "never synced"

    val isCached = (status as? CurrencyViewModel.CurrencyStatus.Success)?.isOffline == true
    return if (isCached) "Updated $timeStr · cached" else "Updated $timeStr"
}

/**
 * "1 EUR = 91.20 USD" style per-row unit-rate subline for the "in your currencies" list
 * (DhruvNext §6.6) — the rate of one unit of [otherCode] expressed in [homeCode]. Returns `null`
 * (so [ListGroupRow] simply omits the subtitle line) rather than a misleading placeholder when
 * either rate is missing.
 */
internal fun buildUnitRateCaption(
    otherCode: String,
    homeCode: String,
    rates: Map<String, Double>,
): String? {
    val unitValue = convertViaUsdBase(1.0, otherCode, homeCode, rates) ?: return null
    return "1 $otherCode = ${formatConvertedAmount(unitValue)} $homeCode"
}

// --- DhruvNext §6.6 pixel values with no equivalent named token in DhruvNextType/DhruvNextRadii
// (see libs/core's DhruvNextTokens.kt) — kept as local literals since only this screen uses them,
// rather than inventing new shared tokens for a single call site.
private val AMOUNT_FONT_SIZE = 28.sp
private val AMOUNT_LETTER_SPACING = (-0.5).sp
private val RATE_CAPTION_FONT_SIZE = 12.sp
private val FRESHNESS_CAPTION_FONT_SIZE = 11.sp
private val EDIT_ACTION_FONT_SIZE = 12.sp
private val TILE_CODE_FONT_SIZE = 10.sp
private val SELECTOR_CODE_FONT_SIZE = 14.sp
private val SELECTOR_NAME_FONT_SIZE = 10.5.sp
private val PREVIEW_TRAILING_FONT_SIZE = 14.sp
private val SELECTOR_TILE_SIZE = 34.dp
private val CONVERTER_CARD_PADDING = 6.dp
private val SWAP_FAB_SIZE = 40.dp
private val SWAP_FAB_END_INSET = 8.dp
private val ROW_HORIZONTAL_PADDING = 10.dp
private val ROW_VERTICAL_PADDING = 12.dp
private val SWAP_FAB_ROW_GUTTER = 52.dp

/** Tablet width cap for the whole screen's content column (matches CalculatorScreen's convention). */
private val CURRENCY_CONTENT_MAX_WIDTH = 480.dp
