package com.dhruv.finance.app.ui.hub

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhruv.core.flags.FeatureFlagResolver
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.ui.FeatureHost
import com.dhruv.finance.currency.CurrencyScreen
import com.dhruv.finance.currency.CurrencyViewModel
import com.dhruv.finance.everyday.EverydayScreen
import com.dhruv.finance.everyday.EverydayViewModel
import com.dhruv.finance.investments.InvestmentsScreen
import com.dhruv.finance.investments.InvestmentsViewModel
import com.dhruv.finance.loans.LoansScreen
import com.dhruv.finance.loans.LoansViewModel
import com.dhruv.finance.tax.TaxScreen
import com.dhruv.finance.tax.TaxViewModel
import com.dhruv.finance.unit.UnitScreen
import com.dhruv.finance.unit.UnitViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Hub composables group several feature modules behind one top-level tab (preserving the original
 * Converter/Finance UX). Each sub-feature screen is independently wrapped in [FeatureHost] so its
 * flag gates it and a crash in one sub-feature renders FeatureErrorCard without taking down the hub.
 */
@Composable
fun ConverterHub(
    resolver: FeatureFlagResolver,
    crashReporter: CrashReporter,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf("currency" to "Currency", "unit" to "Units")
    var selected by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selected) {
            tabs.forEachIndexed { index, (_, label) ->
                Tab(selected = selected == index, onClick = { selected = index }, text = { Text(label) })
            }
        }
        when (tabs[selected].first) {
            "currency" -> {
                val vm: CurrencyViewModel = koinViewModel()
                val error by vm.featureError.collectAsStateWithLifecycle()
                FeatureHost("currency", resolver.isEnabled("currency"), error, crashReporter) {
                    CurrencyScreen(viewModel = vm)
                }
            }
            "unit" -> {
                val vm: UnitViewModel = koinViewModel()
                val error by vm.featureError.collectAsStateWithLifecycle()
                FeatureHost("unit", resolver.isEnabled("unit"), error, crashReporter) {
                    UnitScreen(viewModel = vm)
                }
            }
        }
    }
}

@Composable
fun FinanceHub(
    resolver: FeatureFlagResolver,
    crashReporter: CrashReporter,
    modifier: Modifier = Modifier,
) {
    val tabs =
        listOf(
            "loans" to "Loans",
            "investments" to "Investments",
            "tax" to "Tax",
            "everyday" to "Everyday",
        )
    var selected by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selected, edgePadding = 0.dp) {
            tabs.forEachIndexed { index, (_, label) ->
                Tab(selected = selected == index, onClick = { selected = index }, text = { Text(label) })
            }
        }
        when (tabs[selected].first) {
            "loans" -> {
                val vm: LoansViewModel = koinViewModel()
                val error by vm.featureError.collectAsStateWithLifecycle()
                FeatureHost("loans", resolver.isEnabled("loans"), error, crashReporter) { LoansScreen(viewModel = vm) }
            }
            "investments" -> {
                val vm: InvestmentsViewModel = koinViewModel()
                val error by vm.featureError.collectAsStateWithLifecycle()
                FeatureHost("investments", resolver.isEnabled("investments"), error, crashReporter) { InvestmentsScreen(viewModel = vm) }
            }
            "tax" -> {
                val vm: TaxViewModel = koinViewModel()
                val error by vm.featureError.collectAsStateWithLifecycle()
                FeatureHost("tax", resolver.isEnabled("tax"), error, crashReporter) { TaxScreen(viewModel = vm) }
            }
            "everyday" -> {
                val vm: EverydayViewModel = koinViewModel()
                val error by vm.featureError.collectAsStateWithLifecycle()
                FeatureHost("everyday", resolver.isEnabled("everyday"), error, crashReporter) { EverydayScreen(viewModel = vm) }
            }
        }
    }
}
