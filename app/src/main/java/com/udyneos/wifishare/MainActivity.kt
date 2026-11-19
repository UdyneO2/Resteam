package com.udyneos.wifishare

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udyneos.wifishare.ui.components.GlassBottomBar
import com.udyneos.wifishare.ui.components.GlassHost
import com.udyneos.wifishare.ui.components.GlassTextDialog
import com.udyneos.wifishare.ui.components.LocalGlassOpacity
import com.udyneos.wifishare.ui.components.Tab
import com.udyneos.wifishare.ui.components.TextEditRequest
import com.udyneos.wifishare.ui.screens.ClientScreen
import com.udyneos.wifishare.ui.screens.SettingsScreen
import com.udyneos.wifishare.ui.screens.WifiScreen
import com.udyneos.wifishare.ui.theme.WifiShareTheme

private val DashboardNavyBg = Color(0xFF0B1222)

class MainActivity : ComponentActivity() {

    private val viewModel: ShareViewModel by viewModels()

    // Android 13+ pakai NEARBY_WIFI_DEVICES, di bawahnya WiFi Direct masih butuh izin lokasi
    private val requiredPermission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.NEARBY_WIFI_DEVICES
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }

    private fun requestedPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(requiredPermission, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(requiredPermission)
        }

    private fun hasPermission() =
        ContextCompat.checkSelfPermission(this, requiredPermission) == PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settings = viewModel.settings

        setContent {
            WifiShareTheme {
                var tab by remember { mutableStateOf(Tab.WIFI) }
                val shareState by viewModel.state.collectAsStateWithLifecycle()
                val clientState by viewModel.clientState.collectAsStateWithLifecycle()
                // dialog edit digambar di overlay GlassHost, bukan di dalam layar
                var dialog by remember { mutableStateOf<TextEditRequest?>(null) }

                // izin notifikasi opsional: tanpa itu service tetap jalan, cuma notifikasinya tersembunyi
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    if (results[requiredPermission] == true) viewModel.start()
                }

                val toggle: (Boolean) -> Unit = { on ->
                    when {
                        !on -> viewModel.stop()
                        hasPermission() -> viewModel.start()
                        else -> permissionLauncher.launch(requestedPermissions())
                    }
                }

                LaunchedEffect(Unit) {
                    if (settings.autoStart && hasPermission()) viewModel.start()
                }

                // layar nyala hanya selama berbagi aktif dan opsinya dihidupkan
                LaunchedEffect(shareState.active, settings.keepScreenOn) {
                    if (shareState.active && settings.keepScreenOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                val icons = remember {
                    mapOf(
                        Tab.WIFI to Icons.Rounded.Wifi,
                        Tab.CLIENT to Icons.Rounded.Link,
                        Tab.SETTINGS to Icons.Rounded.Settings,
                    )
                }

                CompositionLocalProvider(LocalGlassOpacity provides settings.glassOpacity) {
                    GlassHost(
                        animateBackground = settings.animateBackground,
                        overlay = { screen ->
                            GlassBottomBar(
                                backdrop = screen,
                                selected = tab,
                                icons = icons,
                                onSelect = { tab = it },
                                modifier = Modifier.align(Alignment.BottomCenter),
                            )
                            // dialog di atas bar, juga membiaskan `screen` (di luar layernya)
                            GlassTextDialog(
                                backdrop = screen,
                                request = dialog,
                                onDismiss = { dialog = null },
                            )
                        },
                    ) { wallpaper ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DashboardNavyBg),
                        ) {
                            when (tab) {
                                Tab.WIFI -> WifiScreen(
                                    wallpaper = wallpaper,
                                    state = shareState,
                                    onToggle = toggle,
                                    bottomInset = 110.dp,
                                )
                                Tab.CLIENT -> ClientScreen(
                                    wallpaper = wallpaper,
                                    settings = settings,
                                    state = clientState,
                                    sharing = shareState.active || shareState.busy,
                                    bottomInset = 110.dp,
                                    onConnect = viewModel::connectClient,
                                    onDisconnect = viewModel::disconnectClient,
                                    onEdit = { dialog = it },
                                )
                                Tab.SETTINGS -> SettingsScreen(
                                    wallpaper = wallpaper,
                                    settings = settings,
                                    sharing = shareState.active,
                                    bottomInset = 110.dp,
                                    onEdit = { dialog = it },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
