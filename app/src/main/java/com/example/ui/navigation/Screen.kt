package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
    object Cleaner : Screen("cleaner", "Cleaner", Icons.Filled.CleaningServices, Icons.Outlined.CleaningServices)
    object Analyzer : Screen("analyzer", "Analyzer", Icons.Filled.PieChart, Icons.Outlined.PieChart)
    object Apps : Screen("apps", "Apps", Icons.Filled.Apps, Icons.Outlined.Apps)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}
