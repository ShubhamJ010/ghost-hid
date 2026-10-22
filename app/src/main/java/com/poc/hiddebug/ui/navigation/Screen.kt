package com.poc.hiddebug.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    Dashboard("dashboard", "Dashboard", Icons.Default.Dashboard),
    Combos("combos", "Combos", Icons.Default.Code),
    Snippets("snippets", "Snippets", Icons.Default.Key),
    Trackpad("trackpad", "Trackpad", Icons.Default.Mouse)
}
