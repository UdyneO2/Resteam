package com.example.launcher.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.launcher.ui.home.HomeScreen
import com.example.launcher.ui.theme.LauncherColors
import com.example.launcher.ui.theme.LauncherShapes

/**
 * Shell adaptive: layar kompak (HP) memakai bottom bar,
 * layar medium/expanded (tablet, foldable) memakai sidebar seperti desain referensi.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun LauncherApp(windowSizeClass: WindowSizeClass) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = AppDestination.fromRoute(backStackEntry?.destination?.route)
    val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    val navigate: (AppDestination) -> Unit = { destination ->
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (isCompact) {
        Scaffold(
            containerColor = LauncherColors.Canvas,
            bottomBar = { LauncherBottomBar(current, navigate) },
        ) { padding ->
            LauncherNavHost(
                navController = navController,
                isCompact = true,
                modifier = Modifier.padding(padding).statusBarsPadding(),
            )
        }
    } else {
        Row(
            Modifier
                .fillMaxSize()
                .background(LauncherColors.Canvas)
                .systemBarsPadding(),
        ) {
            LauncherSidebar(current, navigate, Modifier.width(225.dp).fillMaxHeight())
            VerticalDivider(color = LauncherColors.Border)
            LauncherNavHost(navController, isCompact = false, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun LauncherNavHost(
    navController: NavHostController,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Library.route,
        modifier = modifier,
    ) {
        composable(AppDestination.Library.route) { HomeScreen(isCompact = isCompact) }
        composable(AppDestination.Shizuku.route) { PlaceholderScreen("Shizuku Tweaks") }
        composable(AppDestination.Settings.route) { PlaceholderScreen("Settings") }
    }
}

@Composable
private fun LauncherSidebar(
    current: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "LAUNCHER",
            style = MaterialTheme.typography.titleLarge,
            color = LauncherColors.Accent,
            modifier = Modifier.padding(start = 8.dp, bottom = 24.dp),
        )
        AppDestination.entries
            .filter { it != AppDestination.Settings }
            .forEach { SidebarItem(it, it == current, onNavigate) }
        Spacer(Modifier.weight(1f))
        SidebarItem(AppDestination.Settings, current == AppDestination.Settings, onNavigate)
    }
}

@Composable
private fun SidebarItem(
    destination: AppDestination,
    selected: Boolean,
    onNavigate: (AppDestination) -> Unit,
) {
    Surface(
        onClick = { onNavigate(destination) },
        modifier = Modifier.fillMaxWidth(),
        shape = LauncherShapes.Pill,
        color = if (selected) LauncherColors.Surface else Color.Transparent,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                destination.icon,
                contentDescription = null,
                tint = if (selected) LauncherColors.Accent else LauncherColors.TextSecondary,
            )
            Text(
                destination.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) LauncherColors.TextPrimary else LauncherColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun LauncherBottomBar(
    current: AppDestination,
    onNavigate: (AppDestination) -> Unit,
) {
    NavigationBar(containerColor = LauncherColors.Surface) {
        AppDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination == current,
                onClick = { onNavigate(destination) },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = LauncherColors.Border,
                    selectedIconColor = LauncherColors.Accent,
                    selectedTextColor = LauncherColors.Accent,
                    unselectedIconColor = LauncherColors.TextSecondary,
                    unselectedTextColor = LauncherColors.TextSecondary,
                ),
            )
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$title — segera hadir", color = LauncherColors.TextSecondary)
    }
}
