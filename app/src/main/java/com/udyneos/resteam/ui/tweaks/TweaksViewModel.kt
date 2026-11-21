package com.udyneos.resteam.ui.tweaks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udyneos.resteam.domain.model.CompileFilter
import com.udyneos.resteam.domain.model.Game
import com.udyneos.resteam.domain.model.GameMode
import com.udyneos.resteam.domain.model.ShizukuState
import com.udyneos.resteam.domain.model.TweakPreset
import com.udyneos.resteam.domain.repository.GameRepository
import com.udyneos.resteam.domain.repository.PresetRepository
import com.udyneos.resteam.domain.usecase.ApplyPresetUseCase
import com.udyneos.resteam.shizuku.ShizukuManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PresetDraft(
    val id: Long = 0,
    val name: String = "",
    val gameMode: GameMode = GameMode.Standard,
    val compileFilter: CompileFilter? = null,
) {
    val isValid: Boolean get() = name.trim().isNotEmpty()
}

data class TweaksUiState(
    val games: List<Game> = emptyList(),
    val selectedGameId: String? = null,
    val presets: List<TweakPreset> = emptyList(),
    val draft: PresetDraft? = null,
    val isLoading: Boolean = true,
    val isApplying: Boolean = false,
    val canApply: Boolean = false,
)

sealed interface TweaksEvent {
    data class ShowMessage(val text: String) : TweaksEvent
}

@HiltViewModel
class TweaksViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val presetRepository: PresetRepository,
    private val applyPreset: ApplyPresetUseCase,
    private val shizukuManager: ShizukuManager,
) : ViewModel() {

    private val _state = MutableStateFlow(TweaksUiState())
    val uiState: StateFlow<TweaksUiState> = _state.asStateFlow()

    private val _events = Channel<TweaksEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        refresh()
        observePresets()
        observeShizuku()
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { gameRepository.getGames() }
                .onSuccess { games ->
                    _state.update { current ->
                        val selected = current.selectedGameId?.takeIf { id -> games.any { it.id == id } }
                            ?: games.firstOrNull()?.id
                        current.copy(games = games, selectedGameId = selected, isLoading = false)
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    _state.update { it.copy(isLoading = false) }
                    emit(error.message ?: "Gagal memuat game")
                }
        }
    }

    fun onGameSelected(game: Game) = _state.update { it.copy(selectedGameId = game.id) }

    fun onNewPreset() = _state.update { it.copy(draft = PresetDraft()) }

    fun onEditPreset(preset: TweakPreset) = _state.update {
        it.copy(draft = PresetDraft(preset.id, preset.name, preset.gameMode, preset.compileFilter))
    }

    fun onDraftChange(draft: PresetDraft) =
        _state.update { it.copy(draft = draft.copy(name = draft.name.take(MAX_NAME_LENGTH))) }

    fun onDismissDraft() = _state.update { it.copy(draft = null) }

    fun onSaveDraft() {
        val current = _state.value
        val draft = current.draft ?: return
        val gameId = current.selectedGameId ?: return
        if (!draft.isValid) return

        viewModelScope.launch {
            runCatching {
                presetRepository.save(
                    TweakPreset(draft.id, gameId, draft.name.trim(), draft.gameMode, draft.compileFilter),
                )
            }.onSuccess {
                _state.update { it.copy(draft = null) }
                emit("Preset disimpan")
            }.onFailure { error ->
                if (error is CancellationException) throw error
                emit(error.message ?: "Gagal menyimpan preset")
            }
        }
    }

    fun onDeletePreset(preset: TweakPreset) {
        viewModelScope.launch {
            runCatching { presetRepository.delete(preset.id) }
                .onSuccess { emit("Preset dihapus") }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    emit(error.message ?: "Gagal menghapus preset")
                }
        }
    }

    fun onApplyPreset(preset: TweakPreset) {
        val current = _state.value
        val game = current.games.firstOrNull { it.id == current.selectedGameId } ?: return
        if (current.isApplying) return
        if (!current.canApply) {
            emit("Shizuku belum siap")
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isApplying = true) }
            val result = applyPreset(preset, game.packageName)
            _state.update { it.copy(isApplying = false) }
            emit(
                result.fold(
                    onSuccess = { "Preset \"${preset.name}\" diterapkan ke ${game.title}" },
                    onFailure = { it.message ?: "Gagal menerapkan preset" },
                ),
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observePresets() {
        viewModelScope.launch {
            _state
                .map { it.selectedGameId }
                .distinctUntilChanged()
                .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else presetRepository.observePresets(id) }
                .collect { presets -> _state.update { it.copy(presets = presets) } }
        }
    }

    private fun observeShizuku() {
        viewModelScope.launch {
            shizukuManager.state.collect { state ->
                _state.update { it.copy(canApply = state is ShizukuState.Ready) }
            }
        }
    }

    private fun emit(text: String) {
        _events.trySend(TweaksEvent.ShowMessage(text))
    }

    companion object {
        const val MAX_NAME_LENGTH = 24
    }
}
