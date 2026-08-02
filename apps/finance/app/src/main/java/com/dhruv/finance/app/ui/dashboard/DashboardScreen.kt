package com.dhruv.finance.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dhruv.core.ui.components.DhruvLogo
import com.dhruv.core.ui.components.EmptyStateCard
import com.dhruv.core.ui.theme.DhruvNextSpacing

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(DhruvNextSpacing.screenGutter),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DhruvLogo(modifier = Modifier.height(DhruvNextSpacing.sectionGap * 3))
        Spacer(modifier = Modifier.height(DhruvNextSpacing.interCardGap))
        EmptyStateCard(message = "Your personalized overview is coming soon.")
    }
}
