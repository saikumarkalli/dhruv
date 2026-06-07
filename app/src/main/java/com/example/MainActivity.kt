package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.calculator.CalculatorScreen
import com.example.ui.calculator.CalculatorViewModel
import com.example.ui.converter.ConverterScreen
import com.example.ui.converter.ConverterViewModel
import com.example.ui.date.DateScreen
import com.example.ui.date.DateViewModel
import com.example.ui.finance.FinanceScreen
import com.example.ui.finance.FinanceViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SectionTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

// Navigation destination descriptor
private data class NavItem(
    val label: String,
    val icon: ImageVector,
    val contentDescription: String,
    val testTag: String,
    val pageIndex: Int
)

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settingsRepository: com.example.data.SettingsRepository = koinInject()
            val darkModePreference by settingsRepository.darkModePreference.collectAsState()

            val calculatorColor by settingsRepository.calculatorColor.collectAsState()
            val converterColor by settingsRepository.converterColor.collectAsState()
            val dateColor by settingsRepository.dateColor.collectAsState()
            val financeColor by settingsRepository.financeColor.collectAsState()

            val isConverterEnabled by settingsRepository.isConverterEnabled.collectAsState()
            val isDateEnabled by settingsRepository.isDateEnabled.collectAsState()
            val isFinanceEnabled by settingsRepository.isFinanceEnabled.collectAsState()

            val calculatorViewModel: CalculatorViewModel = koinViewModel()
            val converterViewModel: ConverterViewModel = koinViewModel()
            val dateViewModel: DateViewModel = koinViewModel()
            val financeViewModel: FinanceViewModel = koinViewModel()

            MyApplicationTheme(darkModePreference = darkModePreference) {
                val activeNavItems = remember(isConverterEnabled, isDateEnabled, isFinanceEnabled) {
                    buildList {
                        add(NavItem("Calc", Icons.Default.Calculate, "Calculator", "nav_item_calculator", pageIndex = 0))
                        if (isConverterEnabled) {
                            add(NavItem("Converter", Icons.Default.SwapHoriz, "Converter", "nav_item_converter", pageIndex = 1))
                        }
                        if (isDateEnabled) {
                            add(NavItem("Date", Icons.Default.DateRange, "Date", "nav_item_date", pageIndex = 2))
                        }
                        if (isFinanceEnabled) {
                            add(NavItem("Finance", Icons.AutoMirrored.Filled.TrendingUp, "Finance", "nav_item_finance", pageIndex = 3))
                        }
                        add(NavItem("Settings", Icons.Default.Settings, "Settings", "nav_item_settings", pageIndex = 4))
                    }
                }

                val pagerState = rememberPagerState(pageCount = { activeNavItems.size })
                val coroutineScope = rememberCoroutineScope()

                // ── Back-press: navigate to Calc tab instead of closing the app ──
                DisposableEffect(pagerState.currentPage) {
                    val callback = object : OnBackPressedCallback(pagerState.currentPage != 0) {
                        override fun handleOnBackPressed() {
                            coroutineScope.launch {
                                if (pagerState.currentPage > 1) {
                                    pagerState.scrollToPage(0)
                                } else {
                                    pagerState.animateScrollToPage(0)
                                }
                            }
                        }
                    }
                    onBackPressedDispatcher.addCallback(this@MainActivity, callback)
                    onDispose { callback.remove() }
                }

                val activeAccentName = if (pagerState.currentPage in activeNavItems.indices) {
                    when (activeNavItems[pagerState.currentPage].pageIndex) {
                        0 -> calculatorColor
                        1 -> converterColor
                        2 -> dateColor
                        3 -> financeColor
                        else -> "cyan"
                    }
                } else {
                    "cyan"
                }

                val isDarkTheme = when (darkModePreference) {
                    "always_dark" -> true
                    "always_light" -> false
                    else -> isSystemInDarkTheme()
                }
                val activeAccentColor by animateColorAsState(
                    targetValue = com.example.ui.theme.getAccentColor(activeAccentName, isDarkTheme),
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "accentColor"
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            NavigationBar(
                                modifier = Modifier
                                    .testTag("app_navigation_bar"),
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp
                            ) {
                                activeNavItems.forEachIndexed { index, item ->
                                    val selected = pagerState.currentPage == index
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            coroutineScope.launch {
                                                if (kotlin.math.abs(pagerState.currentPage - index) > 1) {
                                                    pagerState.scrollToPage(index)
                                                } else {
                                                    pagerState.animateScrollToPage(index)
                                                }
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = item.contentDescription
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = item.label,
                                                fontSize = 11.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1
                                            )
                                        },
                                        alwaysShowLabel = true,
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = activeAccentColor,
                                            selectedTextColor = activeAccentColor,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                            indicatorColor = activeAccentColor.copy(alpha = 0.14f)
                                        ),
                                        modifier = Modifier.testTag(item.testTag)
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .statusBarsPadding()
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("app_horizontal_pager")
                        ) { page ->
                            if (page in activeNavItems.indices) {
                                when (activeNavItems[page].pageIndex) {
                                    0 -> SectionTheme(colorPreference = calculatorColor, darkModePreference = darkModePreference) {
                                        CalculatorScreen(viewModel = calculatorViewModel)
                                    }
                                    1 -> SectionTheme(colorPreference = converterColor, darkModePreference = darkModePreference) {
                                        ConverterScreen(viewModel = converterViewModel)
                                    }
                                    2 -> SectionTheme(colorPreference = dateColor, darkModePreference = darkModePreference) {
                                        DateScreen(viewModel = dateViewModel)
                                    }
                                    3 -> SectionTheme(colorPreference = financeColor, darkModePreference = darkModePreference) {
                                        FinanceScreen(viewModel = financeViewModel)
                                    }
                                    4 -> SettingsScreen(
                                        settingsRepository = settingsRepository,
                                        calculatorViewModel = calculatorViewModel,
                                        appVersion = BuildConfig.VERSION_NAME,
                                        appVersionCode = BuildConfig.VERSION_CODE
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
