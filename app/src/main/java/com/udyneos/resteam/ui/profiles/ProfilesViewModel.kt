package com.udyneos.resteam.ui.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udyneos.resteam.domain.model.Game
import com.udyneos.resteam.domain.model.GameProfile
import com.udyneos.resteam.domain.model.TweakPreset
import com.udyneos.resteam.domain.repository.GameRepository
import com.udyneos.resteam.domain.repository.PresetRepository
import com.udyneos.resteam.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileRow(
    val game: Game,
    val presets: List<TweakPreset>,
    val profile: GameProfile?,
) {
    /** Null jika belum dipilih atau preset yang dipilih sudah dihapus. */
    val selectedPresetId: Long? get() = profile?.presetId?.takeIf { id -> presets.any { it.id == id } }
    val autoApply: Boolean get() = profile?.autoApply == true && selectedPresetId != null
}

data class ProfilesUiState(
    val rows: List<ProfileRow> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val presetRepository: PresetRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    /** Null = belum selesai dimuat. */
    private val games = MutableStateFlow<List<Game>?>(null)

    val uiState: StateFlow<ProfilesUiState> = combine(
        games,
        presetRepository.observeAllPresets(),
        profileRepository.observeProfiles(),
    ) { games, presets, profiles ->
        if (games == null) {
            ProfilesUiState()
        } else {
            ProfilesUiState(
                rows = games.map { game ->
                    ProfileRow(game, presets.filter { it.gameId == game.id }, profiles[game.id])
                },
                isLoading = false,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfilesUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching {
                gameRepository.getGames().also { list -> list.forEach { presetRepository.seedDefaultsOnce(it.id) } }
            }.onSuccess { games.value = it }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    games.value = emptyList()
                }
        }
    }

    fun onPresetSelected(game: Game, presetId: Long?) {
        val autoApply = presetId != null && currentRow(game)?.profile?.autoApply == true
        save(GameProfile(game.id, presetId, autoApply))
    }

    fun onAutoApplyChange(game: Game, enabled: Boolean) {
        val presetId = currentRow(game)?.selectedPresetId ?: return
        save(GameProfile(game.id, presetId, enabled))
    }

    private fun currentRow(game: Game): ProfileRow? = uiState.value.rows.firstOrNull { it.game.id == game.id }

    private fun save(profile: GameProfile) {
        viewModelScope.launch { profileRepository.save(profile) }
    }
}
