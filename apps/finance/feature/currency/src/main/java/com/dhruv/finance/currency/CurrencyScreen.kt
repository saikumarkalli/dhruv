package com.dhruv.finance.currency

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.components.DhruvModalSheet
import com.dhruv.core.ui.components.ListGroup
import com.dhruv.core.ui.components.ListGroupRow
import com.dhruv.core.ui.components.NxCard
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
 * DhruvNext §6.6 shape: a two-row converter card (From/To) with a central swap
 * button overlapping both rows, quick-amount [Pill] chips, a rate/staleness
 * caption, a "your currencies" [ListGroup] preview, and an [OfflineBanner]
 * disclosure when running on cached rates.
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

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DhruvNextSpacing.screenGutter, vertical = DhruvNextSpacing.screenGutter),
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

        // Two-row converter card (From/To) with a central swap button overlapping both rows.
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap)) {
                NxCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionLabel(text = "From")
                        CurrencySelectorRow(
                            code = currencyFrom,
                            onClick = { showFromPicker = true },
                            modifier = Modifier.testTag("currency_from_btn"),
                        )
                        OutlinedTextField(
                            value = currencyInput,
                            onValueChange = { viewModel.setCurrencyInput(it) },
                            label = { Text("Amount") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(DhruvNextRadii.innerTile),
                            textStyle =
                                TextStyle(fontSize = DhruvNextType.title, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                            colors =
                                OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.acc,
                                    unfocusedBorderColor = colors.line,
                                    focusedTextColor = colors.tx,
                                    unfocusedTextColor = colors.tx,
                                    focusedLabelColor = colors.acc,
                                    unfocusedLabelColor = colors.tx2,
                                    cursorColor = colors.acc,
                                ),
                            modifier = Modifier.fillMaxWidth().testTag("currency_input_field"),
                        )
                    }
                }

                NxCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionLabel(text = "To")
                        CurrencySelectorRow(
                            code = currencyTo,
                            onClick = { showToPicker = true },
                            modifier = Modifier.testTag("currency_to_btn"),
                        )
                        Text(
                            text = currencyResult.ifBlank { "0" },
                            color = colors.tx,
                            fontSize = DhruvNextType.hero,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(fontFeatureSettings = "tnum"),
                            modifier = Modifier.testTag("currency_output_val"),
                        )
                    }
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
                        .align(Alignment.Center)
                        .shadow(elevation = 2.dp, shape = CircleShape)
                        .size(48.dp)
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

        // Rate + staleness caption.
        val rateCaption =
            remember(currencyFrom, currencyTo, ratesMap, lastUpdatedTime, currencyStatus) {
                buildRateCaption(currencyFrom, currencyTo, ratesMap, lastUpdatedTime, currencyStatus)
            }
        Text(text = rateCaption, color = colors.tx2, fontSize = DhruvNextType.meta)

        // "<amount> <currency> in your currencies" preview list.
        val otherCurrencies =
            remember(currencyFrom, currencyTo) {
                PREFERRED_OTHER_CURRENCIES
                    .filter { it in viewModel.availableCurrencies && it != currencyFrom && it != currencyTo }
                    .take(MAX_OTHER_CURRENCIES)
            }
        if (ratesMap.isNotEmpty() && otherCurrencies.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(text = "$currencyInput $currencyFrom in your currencies")
                ListGroup(
                    rows =
                        otherCurrencies.map { code ->
                            {
                                ListGroupRow(
                                    title = code,
                                    showChevron = false,
                                    trailing = {
                                        val converted =
                                            convertViaUsdBase(currencyInput.toDoubleOrNull(), currencyFrom, code, ratesMap)
                                        Text(
                                            text = converted?.let { formatConvertedAmount(it) } ?: "—",
                                            color = colors.tx,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = DhruvNextType.cardTitle,
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

/** The tappable "USD ▾" / "EUR ▾" currency code affordance atop each converter row. */
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
        Text(text = code, color = colors.tx, fontSize = DhruvNextType.title, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = colors.tx2,
            modifier = Modifier.size(20.dp),
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

/** "1 USD ≈ 0.92 EUR  ·  Updated 5m ago" — rate + cache-staleness caption. */
private fun buildRateCaption(
    fromCode: String,
    toCode: String,
    rates: Map<String, Double>,
    lastUpdatedTime: Long?,
    status: CurrencyViewModel.CurrencyStatus,
): String {
    val fromRate = rates[fromCode]
    val toRate = rates[toCode]
    val rateText =
        if (fromRate != null && toRate != null && fromRate != 0.0) {
            val perUnit = toRate / fromRate
            val df = DecimalFormat("#,##0.####", DecimalFormatSymbols(Locale.US))
            "1 $fromCode ≈ ${df.format(perUnit)} $toCode"
        } else {
            null
        }

    val timeStr =
        lastUpdatedTime?.let {
            val diffMs = System.currentTimeMillis() - it
            val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs) % 60
            when {
                hours > 0 -> "${hours}h ${minutes}m ago"
                minutes > 0 -> "${minutes}m ago"
                else -> "just now"
            }
        } ?: "never synced"

    val freshnessPrefix = if ((status as? CurrencyViewModel.CurrencyStatus.Success)?.isOffline == true) "Cached" else "Updated"

    return listOfNotNull(rateText, "$freshnessPrefix $timeStr").joinToString("  ·  ")
}
