package com.sj010.ghid.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val isBottomNav: Boolean = true
) {
    Dashboard("dashboard", "Dashboard", Icons.Default.Dashboard, isBottomNav = true),
    Combos("combos", "Combos", Icons.Default.Code, isBottomNav = true),
    Trackpad("trackpad", "Trackpad", Icons.Default.Mouse, isBottomNav = true),
    Settings("settings", "Settings", Icons.Default.Settings, isBottomNav = false);

    companion object {
        val bottomNavScreens: List<Screen>
            get() = entries.filter { it.isBottomNav }
    }
}
