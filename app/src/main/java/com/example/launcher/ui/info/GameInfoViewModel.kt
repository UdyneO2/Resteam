package com.example.launcher.ui.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.launcher.domain.model.InfoRow
import com.example.launcher.domain.model.ShizukuState
import com.example.launcher.domain.repository.GameInfoRepository
import com.example.launcher.domain.repository.ShellExecutor
import com.example.launcher.domain.usecase.isValidPackageName
import com.example.launcher.shizuku.ShizukuManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface GameModeInfo {
    data object Loading : GameModeInfo
    data class Loaded(val output: String) : GameModeInfo
    data class Unavailable(val reason: String) : GameModeInfo
}

data class GameInfoUiState(
    val device: List<InfoRow> = emptyList(),
    val app: List<InfoRow> = emptyList(),
    val gameMode: GameModeInfo = GameModeInfo.Loading,
)

@HiltViewModel
class GameInfoViewModel @Inject constructor(
    private val infoRepository: GameInfoRepository,
    private val shell: ShellExecutor,
    private val shizukuManager: ShizukuManager,
) : ViewModel() {

    private val _state = MutableStateFlow(GameInfoUiState())
    val uiState: StateFlow<GameInfoUiState> = _state.asStateFlow()

    fun load(packageName: String) {
        viewModelScope.launch {
            _state.value = GameInfoUiState(
                device = infoRepository.getDeviceInfo(),
                app = infoRepository.getAppInfo(packageName),
                gameMode = GameModeInfo.Loading,
            )
            val gameMode = readGameMode(packageName)
            _state.update { it.copy(gameMode = gameMode) }
        }
    }

    /** Perintah read-only; hanya dijalankan jika Shizuku siap dan package valid. */
    private suspend fun readGameMode(packageName: String): GameModeInfo {
        if (!isValidPackageName(packageName)) return GameModeInfo.Unavailable("Nama package tidak valid")
        if (shizukuManager.state.first() !is ShizukuState.Ready) {
            return GameModeInfo.Unavailable("Aktifkan Shizuku untuk melihat game mode saat ini.")
        }
        val result = shell.run(listOf("cmd", "game", "list-modes", packageName)).getOrElse { error ->
            return GameModeInfo.Unavailable(error.message ?: "Gagal menjalankan perintah")
        }
        return if (result.isSuccess && result.output.isNotBlank()) {
            GameModeInfo.Loaded(result.output)
        } else {
            GameModeInfo.Unavailable(result.output.ifBlank { "Game mode tidak tersedia di perangkat ini (butuh Android 12+)." })
        }
    }
}
