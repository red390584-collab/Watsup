package com.yourapp.whatsappclone.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.yourapp.whatsappclone.ui.theme.LocalWaExtendedColors
import com.yourapp.whatsappclone.ui.theme.Muted
import com.yourapp.whatsappclone.ui.theme.WaGreen

/** WhatsApp's 2024 bottom navigation layout. */
data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val bottomTabs = listOf(
    BottomTab(Routes.CHATS, "Chats", Icons.Filled.Chat),
    BottomTab(Routes.UPDATES, "Updates", Icons.Filled.Newspaper),
    BottomTab(Routes.COMMUNITIES, "Communities", Icons.Filled.Groups),
    BottomTab(Routes.CALLS, "Calls", Icons.Filled.Call),
)

@Composable
fun WaBottomBar(navController: NavController) {
    val colors = LocalWaExtendedColors.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar(
        containerColor = colors.navigationBar,
        contentColor = colors.textPrimary,
    ) {
        bottomTabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (currentRoute != tab.route) {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (selected) WaGreen else Muted,
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = colors.searchChip.copy(alpha = 0f),
                    selectedTextColor = WaGreen,
                    unselectedTextColor = Muted,
                ),
                modifier = Modifier,
            )
        }
    }
}
