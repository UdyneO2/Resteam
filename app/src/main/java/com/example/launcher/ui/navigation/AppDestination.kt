package com.example.launcher.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(val route: String, val label: String, val icon: ImageVector) {
    Library("library", "Game library", Icons.Rounded.GridView),
    Shizuku("shizuku", "Shizuku", Icons.Rounded.Terminal),
    Settings("settings", "Settings", Icons.Rounded.Settings);

    companion object {
        fun fromRoute(route: String?): AppDestination =
            entries.firstOrNull { it.route == route } ?: Library
    }
}
