// Named for the composables it hosts, not the small data class it also declares.
@file:Suppress("MatchingDeclarationName")

package com.dhruv.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.DhruvNextType
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/** One entry in the [BottomBar] — DhruvNext §5's `TABS` (home/calc/plan/insights). */
data class BottomBarTab(
    val key: String,
    val label: String,
    val icon: ImageVector,
)

/** DhruvNext's 4-tab bottom navigation bar (ADR-0024). */
@Composable
fun BottomBar(
    tabs: List<BottomBarTab>,
    selectedKey: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDhruvNextColors.current
    NavigationBar(modifier = modifier, containerColor = colors.surf) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = tab.key == selectedKey,
                onClick = { onTabSelected(tab.key) },
                icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.acc,
                        selectedTextColor = colors.acc,
                        indicatorColor = colors.accSoft,
                        unselectedIconColor = colors.tx3,
                        unselectedTextColor = colors.tx3,
                    ),
            )
        }
    }
}

/** A back + title top bar for detail/utility routes — DhruvNext §5's non-tab screens. */
@Composable
fun NxTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = LocalDhruvNextColors.current
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.tx)
            }
        } else {
            Spacer(modifier = Modifier.width(16.dp))
        }
        Text(
            text = title,
            color = colors.tx,
            fontSize = DhruvNextType.title,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp).weight(1f),
        )
        Row(content = actions)
    }
}

/** DhruvNext §6.2's Home top bar: logo, greeting/date, notification bell, settings icon. */
@Composable
fun NxHomeTopBar(
    greeting: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    hasUnreadNotifications: Boolean = false,
    onNotificationsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val colors = LocalDhruvNextColors.current
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = greeting, color = colors.tx, fontSize = DhruvNextType.title, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = colors.tx2, fontSize = DhruvNextType.meta)
        }
        IconButton(onClick = onNotificationsClick) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = if (hasUnreadNotifications) colors.acc else colors.tx2,
            )
        }
        IconButton(onClick = onSettingsClick) {
            Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = colors.tx2)
        }
    }
}
