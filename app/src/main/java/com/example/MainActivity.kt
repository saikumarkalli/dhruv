package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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

                val activeAccentName = when (pagerState.currentPage) {
                    0 -> calculatorColor
                    1 -> converterColor
                    2 -> dateColor
                    3 -> financeColor
                    else -> "cyan"
                }
                val isDarkTheme = when (darkModePreference) {
                    "always_dark" -> true
                    "always_light" -> false
                    else -> isSystemInDarkTheme()
                }
                val activeAccentColor = com.example.ui.theme.getAccentColor(activeAccentName, isDarkTheme)

                val itemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = activeAccentColor,
                    selectedTextColor = activeAccentColor,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    indicatorColor = activeAccentColor.copy(alpha = 0.12f)
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        Column(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.background)
                                .statusBarsPadding()
                        ) {
                            TabRow(
                                selectedTabIndex = pagerState.currentPage,
                                containerColor = MaterialTheme.colorScheme.background,
                                indicator = { tabPositions ->
                                    if (pagerState.currentPage < tabPositions.size) {
                                        TabRowDefaults.SecondaryIndicator(
                                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                            color = activeAccentColor,
                                            height = 3.dp
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("app_navigation_bar")
                            ) {
                                Tab(
                                    selected = pagerState.currentPage == 0,
                                    onClick = {
                                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                                    },
                                    icon = {
                                        Icon(Icons.Default.Calculate, contentDescription = "Calculator tab")
                                    },
                                    text = { Text("Calculator", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    selectedContentColor = activeAccentColor,
                                    unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.testTag("nav_item_calculator")
                                )
                                Tab(
                                    selected = pagerState.currentPage == 1,
                                    onClick = {
                                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                    },
                                    icon = {
                                        Icon(Icons.Default.SwapHoriz, contentDescription = "Converter tab")
                                    },
                                    text = { Text("Converter", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    selectedContentColor = activeAccentColor,
                                    unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.testTag("nav_item_converter")
                                )
                                Tab(
                                    selected = pagerState.currentPage == 2,
                                    onClick = {
                                        coroutineScope.launch { pagerState.animateScrollToPage(2) }
                                    },
                                    icon = {
                                        Icon(Icons.Default.DateRange, contentDescription = "Date tab")
                                    },
                                    text = { Text("Date", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    selectedContentColor = activeAccentColor,
                                    unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.testTag("nav_item_date")
                                )
                                Tab(
                                    selected = pagerState.currentPage == 3,
                                    onClick = {
                                        coroutineScope.launch { pagerState.animateScrollToPage(3) }
                                    },
                                    icon = {
                                        Icon(Icons.Default.TrendingUp, contentDescription = "Finance tab")
                                    },
                                    text = { Text("Finance", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    selectedContentColor = activeAccentColor,
                                    unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                    modifier = Modifier.testTag("nav_item_finance")
                                )
                                Tab(
                                    selected = pagerState.currentPage == 4,
                                    onClick = {
                                        coroutineScope.launch { pagerState.animateScrollToPage(4) }
                                    },
                                    icon = {
                                        Icon(Icons.Default.Settings, contentDescription = "Settings tab")
                                    },
                                    text = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    selectedContentColor = activeAccentColor,
                                    unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                    modifier = Modifier.testTag("nav_item_settings")
                                )
                            }
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
