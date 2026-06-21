package com.dhruv.finance.app

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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhruv.core.flags.FeatureFlagResolver
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.ui.FeatureHost
import com.dhruv.core.ui.components.DhruvWordmarkImage
import com.dhruv.core.ui.theme.AppTheme
import com.dhruv.core.ui.theme.DhruvTheme
import com.dhruv.core.ui.theme.SectionTheme
import com.dhruv.core.ui.theme.getAccentColor
import com.dhruv.finance.assistant.AssistantScreen
import com.dhruv.finance.assistant.AssistantViewModel
import com.dhruv.finance.calculator.CalculatorScreen
import com.dhruv.finance.calculator.CalculatorViewModel
import com.dhruv.finance.date.DateScreen
import com.dhruv.finance.date.DateViewModel
import com.dhruv.finance.time.TimeScreen
import com.dhruv.finance.time.TimeViewModel
import com.dhruv.settings.SettingsRepository
import com.dhruv.finance.app.ui.hub.ConverterHub
import com.dhruv.finance.app.ui.hub.FinanceHub
import com.dhruv.finance.app.ui.settings.SettingsScreen
import com.dhruv.finance.app.ui.settings.SettingsViewModel
import com.dhruv.finance.app.ui.splash.SplashScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * A bottom-nav destination. [content] is the already-FeatureHost-wrapped screen (or a hub of several
 * feature screens). [colorName] selects the per-section accent from user settings (preserved UX).
 */
private data class NavTab(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val colorName: String,
    val content: @Composable () -> Unit,
)

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settingsRepository: SettingsRepository = koinInject()
            val resolver: FeatureFlagResolver = koinInject()
            val crashReporter: CrashReporter = koinInject()
            val settingsViewModel: SettingsViewModel = koinViewModel()

            val appSettings by settingsViewModel.settings.collectAsState()

            // Per-section accent colours (legacy settings flows) — preserved for the bottom nav.
            val darkModePreference by settingsRepository.darkModePreference.collectAsState()
            val calculatorColor by settingsRepository.calculatorColor.collectAsState()
            val converterColor by settingsRepository.converterColor.collectAsState()
            val dateColor by settingsRepository.dateColor.collectAsState()
            val financeColor by settingsRepository.financeColor.collectAsState()
            val timeColor by settingsRepository.timeColor.collectAsState()

            // Calculator VM is shared: the Calc route and the Settings "clear history" action.
            val calculatorViewModel: CalculatorViewModel = koinViewModel()

            DhruvTheme(
                theme = appSettings.theme,
                accentColorHex = appSettings.accentColorHex,
                font = appSettings.fontFamily
            ) {
                // Branded launch splash: show the dhruv wordmark briefly, then reveal the app.
                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(1200)
                    showSplash = false
                }
                if (showSplash) {
                    SplashScreen()
                    return@DhruvTheme
                }

                // Build the visible tabs. Feature flags gate Date / Time / Assistant; Converter and
                // Finance are hubs shown when any of their sub-features is enabled.
                val tabs = remember(resolver) {
                    buildList {
                        add(
                            NavTab("calculator", "Calc", Icons.Default.Calculate, calculatorColor) {
                                val error by calculatorViewModel.featureError.collectAsStateWithLifecycle()
                                FeatureHost("calculator", resolver.isEnabled("calculator"), error, crashReporter) {
                                    CalculatorScreen(viewModel = calculatorViewModel)
                                }
                            }
                        )
                        if (resolver.isEnabled("currency") || resolver.isEnabled("unit")) {
                            add(NavTab("converter", "Converter", Icons.Default.SwapHoriz, converterColor) {
                                ConverterHub(resolver, crashReporter)
                            })
                        }
                        if (resolver.isEnabled("date")) {
                            add(NavTab("date", "Date", Icons.Default.DateRange, dateColor) {
                                val vm: DateViewModel = koinViewModel()
                                val error by vm.featureError.collectAsStateWithLifecycle()
                                FeatureHost("date", true, error, crashReporter) { DateScreen(viewModel = vm) }
                            })
                        }
                        if (listOf("loans", "investments", "tax", "everyday").any { resolver.isEnabled(it) }) {
                            add(NavTab("finance", "Finance", Icons.AutoMirrored.Filled.TrendingUp, financeColor) {
                                FinanceHub(resolver, crashReporter)
                            })
                        }
                        if (resolver.isEnabled("time")) {
                            add(NavTab("time", "Time", Icons.Default.AccessTime, timeColor) {
                                val vm: TimeViewModel = koinViewModel()
                                val error by vm.featureError.collectAsStateWithLifecycle()
                                FeatureHost("time", true, error, crashReporter) { TimeScreen(viewModel = vm) }
                            })
                        }
                        if (resolver.isEnabled("assistant")) {
                            add(NavTab("assistant", "Assistant", Icons.Default.AutoAwesome, "cyan") {
                                val vm: AssistantViewModel = koinViewModel()
                                val error by vm.featureError.collectAsStateWithLifecycle()
                                FeatureHost("assistant", true, error, crashReporter) { AssistantScreen(viewModel = vm) }
                            })
                        }
                        add(NavTab("settings", "Settings", Icons.Default.Settings, "cyan") {
                            SettingsScreen(
                                settingsRepository = settingsRepository,
                                onClearHistory = { calculatorViewModel.clearHistory() }
                            )
                        })
                    }
                }

                val pagerState = rememberPagerState(pageCount = { tabs.size })
                val coroutineScope = rememberCoroutineScope()
                val settingsTabIndex = tabs.indexOfFirst { it.key == "settings" }

                // Back-press: return to the first tab instead of closing the app.
                DisposableEffect(pagerState.currentPage) {
                    val callback = object : OnBackPressedCallback(pagerState.currentPage != 0) {
                        override fun handleOnBackPressed() {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        }
                    }
                    onBackPressedDispatcher.addCallback(this@MainActivity, callback)
                    onDispose { callback.remove() }
                }

                val activeColorName = tabs.getOrNull(pagerState.currentPage)?.colorName ?: "cyan"
                val isDarkTheme = when (appSettings.theme) {
                    AppTheme.DARK -> true
                    AppTheme.LIGHT -> false
                    AppTheme.SYSTEM -> isSystemInDarkTheme()
                }
                val activeAccentColor by animateColorAsState(
                    targetValue = getAccentColor(activeColorName, isDarkTheme),
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "accentColor"
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                DhruvWordmarkImage(height = 26.dp)
                            },
                            actions = {
                                IconButton(
                                    onClick = {
                                        if (settingsTabIndex >= 0) {
                                            coroutineScope.launch { pagerState.animateScrollToPage(settingsTabIndex) }
                                        }
                                    },
                                    modifier = Modifier.testTag("top_bar_settings_button")
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = "Open Settings")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    },
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
                                modifier = Modifier.testTag("app_navigation_bar"),
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp
                            ) {
                                tabs.forEachIndexed { index, tab ->
                                    val selected = pagerState.currentPage == index
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                        },
                                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                                        label = {
                                            Text(
                                                text = tab.label,
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
                                        modifier = Modifier.testTag("nav_item_${tab.key}")
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
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("app_horizontal_pager")
                        ) { page ->
                            val tab = tabs[page]
                            SectionTheme(colorPreference = tab.colorName, darkModePreference = darkModePreference) {
                                tab.content()
                            }
                        }
                    }
                }
            }
        }
    }
}
