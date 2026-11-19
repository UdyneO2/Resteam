package com.udyneos.glasslauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.udyneos.glasslauncher.ui.home.HomeScreen
import com.udyneos.glasslauncher.ui.theme.GlassTheme

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GlassTheme {
                val apps by viewModel.apps.collectAsState()
                val dock by viewModel.dock.collectAsState()
                val showLabels by viewModel.prefs.showLabels.collectAsState()
                val blur by viewModel.prefs.blurDp.collectAsState()
                val tint by viewModel.prefs.tintAlpha.collectAsState()

                HomeScreen(
                    apps = apps,
                    dock = dock,
                    showLabels = showLabels,
                    blurDp = blur,
                    tintAlpha = tint,
                    onLaunch = viewModel.repository::launch,
                    onAppInfo = viewModel.repository::openAppInfo,
                    onUninstall = viewModel.repository::uninstall,
                    onShowLabels = viewModel.prefs::setShowLabels,
                    onBlur = viewModel.prefs::setBlurDp,
                    onTint = viewModel.prefs::setTintAlpha,
                    onReloadApps = viewModel::reload,
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // Home button pressed while we're already on top: nothing to do, the
        // pager keeps its position. Hook kept for the "scroll to first page" update.
    }
}
