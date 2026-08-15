package com.dhruv.finance.date

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhruv.core.ui.components.NxCard
import com.dhruv.core.ui.components.NxIconButton
import com.dhruv.core.ui.components.SectionLabel
import com.dhruv.core.ui.theme.DhruvNextRadii
import com.dhruv.core.ui.theme.DhruvNextSpacing
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors
import com.dhruv.settings.SettingsRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

@Composable
fun DateScreen(
    viewModel: DateViewModel,
    settingsRepository: SettingsRepository = koinInject(),
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    val activeSubCalculator by viewModel.activeSubCalculator.collectAsStateWithLifecycle()

    val visibleSubCalculators by remember(DateCalcItems) {
        combine(
            DateCalcItems.map { item ->
                settingsRepository.isToolEnabled(item.name).map { item to it }
            },
        ) { array ->
            array.filter { it.second }.map { it.first }
        }
    }.collectAsState(initial = DateCalcItems)

    Column(
        modifier = modifier.fillMaxSize().background(colors.bg),
    ) {
        if (activeSubCalculator == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(DhruvNextSpacing.screenGutter),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SectionLabel(text = "Date & time")
                Spacer(modifier = Modifier.height(DhruvNextSpacing.interCardGap))

                Text(
                    text = "Select a tool to begin",
                    fontSize = DhruvNextType.body,
                    color = colors.tx2,
                    modifier = Modifier.padding(bottom = DhruvNextSpacing.sectionGap),
                )

                val rows = visibleSubCalculators.chunked(3)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(DhruvNextSpacing.interCardGap),
                ) {
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            row.forEach { item ->
                                val index = DateCalcItems.indexOf(item)
                                GridDateItemCard(
                                    item = item,
                                    onClick = { viewModel.setActiveSubCalculator(index) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (row.size < 3) {
                                repeat(3 - row.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surf)
                        .padding(horizontal = DhruvNextSpacing.interCardGap, vertical = DhruvNextSpacing.interCardGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NxIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = { viewModel.setActiveSubCalculator(null) },
                        contentDescription = "Back to list",
                        tint = colors.acc,
                    )

                    Spacer(modifier = Modifier.width(DhruvNextSpacing.interCardGap))

                    Column {
                        val activeItem = DateCalcItems[activeSubCalculator ?: 0]
                        Text(
                            text = activeItem.name,
                            fontSize = DhruvNextType.cardTitle,
                            fontWeight = FontWeight.Bold,
                            color = colors.tx,
                        )
                        Text(
                            text = activeItem.description,
                            fontSize = DhruvNextType.meta,
                            color = colors.tx2,
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize().padding(DhruvNextSpacing.screenGutter)) {
                    ActiveSubCalcRender(activeSubCalculator ?: 0, viewModel)
                }
            }
        }
    }
}

@Composable
fun GridDateItemCard(
    item: DateCalcItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    Column(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(DhruvNextRadii.card))
            .clickable(onClick = onClick)
            .padding(vertical = DhruvNextSpacing.interCardGap, horizontal = DhruvNextSpacing.inputGroupGap)
            .testTag("grid_item_${item.name.lowercase().replace(" ", "_").replace("/", "and")}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(colors.accSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.name,
                tint = colors.acc,
                modifier = Modifier.size(28.dp),
            )
        }

        Spacer(modifier = Modifier.height(DhruvNextSpacing.inputGroupGap))

        Text(
            text = item.name,
            fontSize = DhruvNextType.body,
            fontWeight = FontWeight.SemiBold,
            color = colors.tx,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun ActiveSubCalcRender(
    index: Int,
    viewModel: DateViewModel,
) {
    NxCard(modifier = Modifier.fillMaxSize()) {
        when (index) {
            0 -> DateDifferenceView(viewModel)
            1 -> AddSubtractDaysView(viewModel)
            2 -> AgeCalculatorView(viewModel)
            3 -> DateCountdownView()
            4 -> TimeZoneConverterView(viewModel)
            5 -> BusinessWorkingDaysView(viewModel)
            6 -> UnixEpochConverterView(viewModel)
        }
    }
}
