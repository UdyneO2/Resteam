package com.example.launcher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.launcher.data.GameLauncher
import com.example.launcher.domain.model.CommandResult
import com.example.launcher.domain.model.Game
import com.example.launcher.domain.model.LauncherTab
import com.example.launcher.domain.model.ShizukuState
import com.example.launcher.domain.model.TweakPreset
import com.example.launcher.domain.repository.GameRepository
import com.example.launcher.domain.repository.PresetRepository
import com.example.launcher.domain.usecase.ApplyPresetUseCase
import com.example.launcher.shizuku.ShizukuManager
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
    private val applyPreset: ApplyPresetUseCase,
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

    val uiState: StateFlow<HomeUiState> = combine(content, selectedTab, shizukuState, tweaks) { c, t, s, w ->
        HomeUiState(content = c, selectedTab = t, shizuku = s, tweaks = w)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        observeShizuku()
        observePresets()
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
        presetRepository.seedDefaultsIfEmpty(selected.id)
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
        val success = content.value as? ContentState.Success ?: return
        gameLauncher.launch(success.dashboard.selected.packageName).onFailure { error ->
            emit(error.message ?: "Gagal membuka game")
        }
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
    fun onClearLog() = tweaks.update { it.copy(log = emptyList()) }
    fun onInfoClick() = emit("System properties segera hadir")

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
                    log = (it.log + logLines(preset, result)).takeLast(MAX_LOG_LINES),
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

    private fun logLines(preset: TweakPreset, result: Result<List<CommandResult>>): List<String> =
        result.fold(
            onSuccess = { list ->
                list.map { "$ ${it.command}\n${it.output.ifBlank { "(ok)" }}" }
            },
            onFailure = { listOf("✗ ${preset.name}: ${it.message}") },
        )

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

    private fun observeShizuku() {
        viewModelScope.launch {
            shizukuManager.state.collect { shizukuState.value = it }
        }
    }

    private fun emit(text: String) {
        _events.trySend(HomeEvent.ShowMessage(text))
    }

    private companion object {
        const val MAX_LOG_LINES = 100
    }
}
