package com.udyneos.resteam.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udyneos.resteam.data.GameLauncher
import com.udyneos.resteam.domain.model.Game
import com.udyneos.resteam.domain.model.LauncherTab
import com.udyneos.resteam.domain.model.ShizukuState
import com.udyneos.resteam.domain.model.TweakPreset
import com.udyneos.resteam.domain.repository.ActivityLog
import com.udyneos.resteam.domain.repository.GameRepository
import com.udyneos.resteam.domain.repository.PresetRepository
import com.udyneos.resteam.domain.repository.ProfileRepository
import com.udyneos.resteam.domain.repository.SettingsRepository
import com.udyneos.resteam.domain.usecase.ApplyPresetUseCase
import com.udyneos.resteam.shizuku.ShizukuManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: GameRepository,
    private val presetRepository: PresetRepository,
    private val profileRepository: ProfileRepository,
    private val applyPreset: ApplyPresetUseCase,
    private val activityLog: ActivityLog,
    private val settingsRepository: SettingsRepository,
    private val shizukuManager: ShizukuManager,
    private val gameLauncher: GameLauncher,
) : ViewModel() {

    private val content = MutableStateFlow<ContentState>(ContentState.Loading)
    private val selectedTab = MutableStateFlow(LauncherTab.AllGames)
    private val shizukuState = MutableStateFlow<ShizukuState>(ShizukuState.Unavailable)
    private val tweaks = MutableStateFlow(TweakUiState())
    private val selectedGameId = MutableStateFlow<String?>(null)

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val uiState: StateFlow<HomeUiState> = combine(content, selectedTab, shizukuState, tweaks, settingsRepository.settings) { c, t, s, w, st ->
        HomeUiState(content = c, selectedTab = t, shizuku = s, tweaks = w, settings = st)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        observeShizuku()
        observePresets()
        observeLog()
        load()
    }

    fun load(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) content.value = ContentState.Loading
            content.value = runCatching { loadDashboard() }.fold(
                onSuccess = { dashboard ->
                    if (dashboard == null) ContentState.NoGames else ContentState.Success(dashboard)
                },
                onFailure = { error ->
                    if (error is CancellationException) throw error
                    ContentState.Error(error.message ?: "Gagal memuat data")
                },
            )
        }
    }

    /** Dipanggil saat layar kembali aktif (mis. setelah memberi akses penggunaan atau memasang game). */
    fun onResume() {
        if (content.value !is ContentState.Loading) load(showLoading = false)
    }

    fun onGameSelected(game: Game) {
        selectedGameId.value = game.id
        load(showLoading = false)
    }

    private suspend fun loadDashboard(): GameDashboard? {
        val games = repository.getGames()
        val selected = games.firstOrNull { it.id == selectedGameId.value } ?: games.firstOrNull() ?: return null
        presetRepository.seedDefaultsOnce(selected.id)
        return GameDashboard(
            games = games,
            selected = selected,
            activity = repository.getMonthlyActivity(selected.packageName),
            hasUsageAccess = repository.hasUsageAccess(),
        )
    }

    fun onTabSelected(tab: LauncherTab) {
        selectedTab.value = tab
    }

    fun onLaunchGame() {
        val game = (content.value as? ContentState.Success)?.dashboard?.selected ?: return
        viewModelScope.launch {
            applyProfilePreset(game)
            gameLauncher.launch(game.packageName).onFailure { error ->
                emit(error.message ?: "Gagal membuka game")
            }
        }
    }

    /** Terapkan preset default profil (jika auto-apply aktif) sebelum game dibuka; kegagalan tidak menghalangi launch. */
    private suspend fun applyProfilePreset(game: Game) {
        val profile = profileRepository.getProfile(game.id)?.takeIf { it.autoApply } ?: return
        val preset = tweaks.value.presets.firstOrNull { it.id == profile.presetId } ?: return
        if (shizukuState.value !is ShizukuState.Ready) {
            emit("Shizuku belum siap, game dibuka tanpa preset")
            return
        }
        val result = applyPreset(preset, game.packageName)
        result.onFailure { emit("Preset gagal diterapkan: ${it.message ?: "kesalahan tidak diketahui"}") }
    }

    fun onShizukuPillClick() {
        if (shizukuState.value == ShizukuState.PermissionRequired) {
            shizukuManager.requestPermission()
        }
    }

    // ---- Tweak preset ----

    fun onTweakPresetClick() = tweaks.update { it.copy(showPresetSheet = true) }
    fun onDismissPresets() = tweaks.update { it.copy(showPresetSheet = false) }
    fun onLogClick() = tweaks.update { it.copy(showLog = true) }
    fun onDismissLog() = tweaks.update { it.copy(showLog = false) }
    fun onClearLog() = activityLog.clear()
    fun onInfoClick() = tweaks.update { it.copy(showInfo = true) }
    fun onDismissInfo() = tweaks.update { it.copy(showInfo = false) }

    fun onApplyPreset(preset: TweakPreset) {
        val game = (content.value as? ContentState.Success)?.dashboard?.selected ?: return
        if (tweaks.value.isApplying) return
        if (shizukuState.value !is ShizukuState.Ready) {
            emit("Shizuku belum siap")
            return
        }
        viewModelScope.launch {
            tweaks.update { it.copy(isApplying = true) }
            val result = applyPreset(preset, game.packageName)
            tweaks.update {
                it.copy(
                    isApplying = false,
                    showPresetSheet = false,
                )
            }
            emit(
                result.fold(
                    onSuccess = { "Preset \"${preset.name}\" diterapkan" },
                    onFailure = { it.message ?: "Gagal menerapkan preset" },
                ),
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observePresets() {
        viewModelScope.launch {
            content
                .filterIsInstance<ContentState.Success>()
                .map { it.dashboard.selected.id }
                .distinctUntilChanged()
                .flatMapLatest { presetRepository.observePresets(it) }
                .collect { presets -> tweaks.update { it.copy(presets = presets) } }
        }
    }

    private fun observeLog() {
        viewModelScope.launch {
            activityLog.entries.collect { entries -> tweaks.update { it.copy(log = entries) } }
        }
    }

    private fun observeShizuku() {
        viewModelScope.launch {
            shizukuManager.state.collect { shizukuState.value = it }
        }
    }

    private fun emit(text: String) {
        _events.trySend(HomeEvent.ShowMessage(text))
    }
}
