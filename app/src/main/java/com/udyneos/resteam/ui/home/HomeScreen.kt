package com.udyneos.resteam.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udyneos.resteam.domain.model.LauncherTab
import com.udyneos.resteam.domain.model.ShizukuState
import com.udyneos.resteam.ui.components.LauncherCard
import com.udyneos.resteam.ui.info.SystemInfoDialog
import com.udyneos.resteam.ui.logs.LogsTab
import com.udyneos.resteam.ui.profiles.ProfilesTab
import com.udyneos.resteam.ui.scripts.ScriptsTab
import com.udyneos.resteam.ui.theme.LauncherColors
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    isCompact: Boolean,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { text -> scope.launch { snackbarHostState.showSnackbar(text) } }
    val context = LocalContext.current
    val openUsageSettings: () -> Unit = {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    // Segarkan daftar game saat kembali dari Settings atau setelah memasang game baru
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.ShowMessage -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HomeTopBar(
                userName = state.settings.userName,
                selectedTab = state.selectedTab,
                onTabSelected = viewModel::onTabSelected,
                shizukuState = state.shizuku,
                isCompact = isCompact,
            )

            if (state.selectedTab == LauncherTab.ShizukuTweaks) {
                ScriptsTab(onMessage = showMessage)
            } else if (state.selectedTab == LauncherTab.Profiles) {
                ProfilesTab()
            } else if (state.selectedTab == LauncherTab.Logs) {
                LogsTab()
            } else if (state.selectedTab != LauncherTab.AllGames) {
                LauncherCard(Modifier.fillMaxWidth()) {
                    Text("${state.selectedTab.label} — segera hadir", color = LauncherColors.TextSecondary)
                }
            } else {
                when (val content = state.content) {
                    ContentState.Loading -> Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = LauncherColors.Accent)
                    }
                    ContentState.NoGames -> MessageCard(
                        message = "Belum ada game terdeteksi. Game yang dikategorikan sebagai game oleh aplikasinya akan muncul otomatis.",
                        actionLabel = "Segarkan",
                        onAction = { viewModel.load() },
                    )
                    is ContentState.Error -> MessageCard(
                        message = content.message,
                        actionLabel = "Coba lagi",
                        onAction = { viewModel.load() },
                        isError = true,
                    )
                    is ContentState.Success -> Dashboard(content.dashboard, state, isCompact, viewModel, openUsageSettings, showMessage)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
    }

    if (state.tweaks.showPresetSheet) {
        PresetSheet(
            presets = state.tweaks.presets,
            isApplying = state.tweaks.isApplying,
            canApply = state.shizuku is ShizukuState.Ready,
            confirmApply = state.settings.confirmBeforeApply,
            onApply = viewModel::onApplyPreset,
            onDismiss = viewModel::onDismissPresets,
        )
    }
    if (state.tweaks.showLog) {
        LogDialog(state.tweaks.log, onClear = viewModel::onClearLog, onDismiss = viewModel::onDismissLog)
    }
    if (state.tweaks.showInfo) {
        (state.content as? ContentState.Success)?.dashboard?.selected?.let { game ->
            SystemInfoDialog(
                packageName = game.packageName,
                gameTitle = game.title,
                onDismiss = viewModel::onDismissInfo,
            )
        }
    }
}

@Composable
private fun Dashboard(
    dashboard: GameDashboard,
    state: HomeUiState,
    isCompact: Boolean,
    viewModel: HomeViewModel,
    onOpenUsageSettings: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val bannerViewModel: GameBannerViewModel = hiltViewModel()
    val banners by bannerViewModel.banners.collectAsStateWithLifecycle()
    val selectedPackage by rememberUpdatedState(dashboard.selected.packageName)
    val bannerPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) bannerViewModel.setBanner(selectedPackage, uri, onMessage)
    }
    HeroBanner(
        bannerFile = banners[dashboard.selected.packageName],
        onBannerClick = {
            bannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onBannerLongClick = { bannerViewModel.clearBanner(dashboard.selected.packageName) },
        game = dashboard.selected,
        shizukuState = state.shizuku,
        isCompact = isCompact,
        onLaunch = viewModel::onLaunchGame,
        onShizukuClick = viewModel::onShizukuPillClick,
        onTweakClick = viewModel::onTweakPresetClick,
        onInfoClick = viewModel::onInfoClick,
        onLogClick = viewModel::onLogClick,
    )
    GameShelf(
        games = dashboard.games,
        selectedId = dashboard.selected.id,
        onSelect = viewModel::onGameSelected,
    )
    if (dashboard.hasUsageAccess) {
        val month = remember { LocalDate.now().month.getDisplayName(TextStyle.FULL, Locale.getDefault()) }
        ActivityCard("Activity in $month", dashboard.activity, Modifier.fillMaxWidth())
    } else {
        MessageCard(
            message = "Aktifkan akses penggunaan agar jam main dan riwayat aktivitas tampil. Data tetap di perangkat.",
            actionLabel = "Buka pengaturan",
            onAction = onOpenUsageSettings,
        )
    }
}

@Composable
private fun MessageCard(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    isError: Boolean = false,
) {
    LauncherCard(Modifier.fillMaxWidth()) {
        Text(message, color = if (isError) LauncherColors.Error else LauncherColors.TextSecondary)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onAction) { Text(actionLabel) }
    }
}
