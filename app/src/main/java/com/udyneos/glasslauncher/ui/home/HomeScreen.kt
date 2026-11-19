package com.udyneos.glasslauncher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.udyneos.glasslauncher.data.AppInfo
import com.udyneos.glasslauncher.ui.glass.glass
import com.udyneos.glasslauncher.ui.library.AppLibrary
import com.udyneos.glasslauncher.ui.settings.SettingsSheet
import kotlin.math.abs

const val COLUMNS = 4
const val ROWS = 6
const val APPS_PER_PAGE = COLUMNS * ROWS

@Composable
fun HomeScreen(
    apps: List<AppInfo>,
    dock: List<AppInfo>,
    showLabels: Boolean,
    blurDp: Float,
    tintAlpha: Float,
    onLaunch: (AppInfo) -> Unit,
    onAppInfo: (AppInfo) -> Unit,
    onUninstall: (AppInfo) -> Unit,
    onShowLabels: (Boolean) -> Unit,
    onBlur: (Float) -> Unit,
    onTint: (Float) -> Unit,
    onReloadApps: () -> Unit,
) {
    var menuApp by remember { mutableStateOf<AppInfo?>(null) }
    var settingsOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = menuApp != null || settingsOpen) {
        if (menuApp != null) menuApp = null else settingsOpen = false
    }

    // Apps that live in the dock shouldn't show up again on the grid.
    val dockKeys = remember(dock) { dock.map { it.key }.toSet() }
    val pages = remember(apps, dockKeys) {
        apps.filterNot { it.key in dockKeys }
            .chunked(APPS_PER_PAGE)
            .ifEmpty { listOf(emptyList()) }
    }

    val pagerState = rememberPagerState(pageCount = { pages.size + 1 })

    // Everything the glass should "see" through lives inside this layer.
    val backdrop = rememberLayerBackdrop {
        drawRect(Color.Black)
        drawContent()
    }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            Wallpaper()

            Column(
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(bottom = 128.dp)
            ) {
                // The clock fades out as soon as you leave the first page.
                ClockHeader(
                    onSecretUnlocked = { settingsOpen = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val offset = pagerState.currentPage + pagerState.currentPageOffsetFraction
                            alpha = (1f - abs(offset)).coerceIn(0f, 1f)
                        },
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                ) { index ->
                    if (index < pages.size) {
                        AppGridPage(
                            apps = pages[index],
                            showLabels = showLabels,
                            onLaunch = onLaunch,
                            onLongPress = { menuApp = it },
                        )
                    } else {
                        AppLibrary(
                            apps = apps,
                            showLabels = showLabels,
                            onLaunch = onLaunch,
                            onLongPress = { menuApp = it },
                        )
                    }
                }
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PageDots(count = pages.size + 1, current = pagerState.currentPage)
            Row(
                Modifier
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .fillMaxWidth()
                    .height(88.dp)
                    .glass(
                        backdrop = backdrop,
                        shape = RoundedCornerShape(32.dp),
                        blur = blurDp.dp,
                        tint = Color.White.copy(alpha = tintAlpha),
                        lensHeight = 14.dp,
                        lensAmount = 28.dp,
                    )
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                dock.forEach { app ->
                    AppIcon(
                        app = app,
                        showLabel = false,
                        onClick = { onLaunch(app) },
                        iconSize = 60.dp,
                        onLongClick = { menuApp = app },
                    )
                }
            }
        }

        menuApp?.let { app ->
            AppContextMenu(
                app = app,
                backdrop = backdrop,
                blurDp = blurDp,
                onDismiss = { menuApp = null },
                onAppInfo = { menuApp = null; onAppInfo(app) },
                onUninstall = { menuApp = null; onUninstall(app) },
            )
        }

        if (settingsOpen) {
            SettingsSheet(
                backdrop = backdrop,
                showLabels = showLabels,
                blurDp = blurDp,
                tintAlpha = tintAlpha,
                onShowLabels = onShowLabels,
                onBlur = onBlur,
                onTint = onTint,
                onReloadApps = onReloadApps,
                onDismiss = { settingsOpen = false },
            )
        }
    }
}

@Composable
private fun AppGridPage(
    apps: List<AppInfo>,
    showLabels: Boolean,
    onLaunch: (AppInfo) -> Unit,
    onLongPress: (AppInfo) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        // Rows split the available height evenly so six rows always fit.
        val rowHeight = maxHeight / ROWS
        Column {
            apps.chunked(COLUMNS).forEach { row ->
                Row(Modifier.height(rowHeight)) {
                    row.forEach { app ->
                        AppIcon(
                            app = app,
                            showLabel = showLabels,
                            onClick = { onLaunch(app) },
                            onLongClick = { onLongPress(app) },
                            modifier = Modifier.weight(1f).fillMaxSize(),
                        )
                    }
                    // Keep short rows left-aligned instead of stretching icons.
                    repeat(COLUMNS - row.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun PageDots(count: Int, current: Int) {
    if (count <= 1) return
    Row(
        Modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        repeat(count) { i ->
            Box(
                Modifier
                    .size(7.dp)
                    .background(
                        Color.White.copy(alpha = if (i == current) 0.95f else 0.4f),
                        CircleShape,
                    )
            )
        }
    }
}
