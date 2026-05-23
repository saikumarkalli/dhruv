package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.calculator.CalculatorScreen
import com.example.ui.calculator.CalculatorViewModel
import com.example.ui.converter.ConverterScreen
import com.example.ui.converter.ConverterViewModel
import com.example.ui.date.DateScreen
import com.example.ui.finance.FinanceScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SectionTheme
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.DateRange

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as CalculatorApplication

        setContent {
            val settingsRepository = app.settingsRepository
            val darkModePreference by settingsRepository.darkModePreference.collectAsState()

            val calculatorColor by settingsRepository.calculatorColor.collectAsState()
            val converterColor by settingsRepository.converterColor.collectAsState()
            val dateColor by settingsRepository.dateColor.collectAsState()
            val financeColor by settingsRepository.financeColor.collectAsState()

            val calculatorViewModel: CalculatorViewModel by viewModels {
                CalculatorViewModel.Factory(app.historyRepository, settingsRepository)
            }

            val converterViewModel: ConverterViewModel by viewModels {
                ConverterViewModel.Factory(app.currencyRepository)
            }

            MyApplicationTheme(darkModePreference = darkModePreference) {
                val pagerState = rememberPagerState(pageCount = { 5 })
                val coroutineScope = rememberCoroutineScope()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.testTag("app_navigation_bar")
                        ) {
                            NavigationBarItem(
                                selected = pagerState.currentPage == 0,
                                onClick = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(0) }
                                },
                                icon = {
                                    Icon(Icons.Default.Calculate, contentDescription = "Calculator tab")
                                },
                                label = { Text("Calculator") },
                                modifier = Modifier.testTag("nav_item_calculator")
                            )
                            NavigationBarItem(
                                selected = pagerState.currentPage == 1,
                                onClick = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                },
                                icon = {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = "Converter tab")
                                },
                                label = { Text("Converter") },
                                modifier = Modifier.testTag("nav_item_converter")
                            )
                            NavigationBarItem(
                                selected = pagerState.currentPage == 2,
                                onClick = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(2) }
                                },
                                icon = {
                                    Icon(Icons.Default.DateRange, contentDescription = "Date and Time tab")
                                },
                                label = { Text("Date & Time") },
                                modifier = Modifier.testTag("nav_item_date")
                            )
                            NavigationBarItem(
                                selected = pagerState.currentPage == 3,
                                onClick = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(3) }
                                },
                                icon = {
                                    Icon(Icons.Default.TrendingUp, contentDescription = "Finance tab")
                                },
                                label = { Text("Finance") },
                                modifier = Modifier.testTag("nav_item_finance")
                            )
                            NavigationBarItem(
                                selected = pagerState.currentPage == 4,
                                onClick = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(4) }
                                },
                                icon = {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings tab")
                                },
                                label = { Text("Settings") },
                                modifier = Modifier.testTag("nav_item_settings")
                            )
                        }
                    }
                ) { innerPadding ->
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .testTag("app_horizontal_pager")
                    ) { page ->
                        when (page) {
                            0 -> SectionTheme(colorPreference = calculatorColor, darkModePreference = darkModePreference) {
                                CalculatorScreen(viewModel = calculatorViewModel)
                            }
                            1 -> SectionTheme(colorPreference = converterColor, darkModePreference = darkModePreference) {
                                ConverterScreen(viewModel = converterViewModel)
                            }
                            2 -> SectionTheme(colorPreference = dateColor, darkModePreference = darkModePreference) {
                                DateScreen()
                            }
                            3 -> SectionTheme(colorPreference = financeColor, darkModePreference = darkModePreference) {
                                FinanceScreen()
                            }
                            4 -> SettingsScreen(
                                settingsRepository = settingsRepository,
                                calculatorViewModel = calculatorViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}
