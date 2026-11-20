package com.example.launcher.ui.home

import com.example.launcher.domain.model.AppSettings
import com.example.launcher.domain.model.Game
import com.example.launcher.domain.model.LogEntry
import com.example.launcher.domain.model.LauncherTab
import com.example.launcher.domain.model.ShizukuState
import com.example.launcher.domain.model.TweakPreset

data class GameDashboard(
    val games: List<Game>,
    val selected: Game,
    val activity: List<Float?>,
    val hasUsageAccess: Boolean,
)

sealed interface ContentState {
    data object Loading : ContentState
    data object NoGames : ContentState
    data class Success(val dashboard: GameDashboard) : ContentState
    data class Error(val message: String) : ContentState
}

/** State untuk bottom sheet preset dan dialog log. */
data class TweakUiState(
    val presets: List<TweakPreset> = emptyList(),
    val showPresetSheet: Boolean = false,
    val isApplying: Boolean = false,
    val log: List<LogEntry> = emptyList(),
    val showLog: Boolean = false,
    val showInfo: Boolean = false,
)

data class HomeUiState(
    val content: ContentState = ContentState.Loading,
    val selectedTab: LauncherTab = LauncherTab.AllGames,
    val shizuku: ShizukuState = ShizukuState.Unavailable,
    val tweaks: TweakUiState = TweakUiState(),
    val settings: AppSettings = AppSettings(),
)

sealed interface HomeEvent {
    data class ShowMessage(val text: String) : HomeEvent
}
