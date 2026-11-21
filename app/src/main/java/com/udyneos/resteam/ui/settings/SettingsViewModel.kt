package com.udyneos.resteam.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udyneos.resteam.domain.model.AppSettings
import com.udyneos.resteam.domain.repository.GameRepository
import com.udyneos.resteam.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val hasUsageAccess: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val gameRepository: GameRepository,
) : ViewModel() {

    private val usageAccess = MutableStateFlow(gameRepository.hasUsageAccess())

    val uiState: StateFlow<SettingsUiState> = combine(settingsRepository.settings, usageAccess) { settings, usage ->
        SettingsUiState(settings = settings, hasUsageAccess = usage)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun onSaveUserName(raw: String) {
        val name = raw.trim().take(MAX_NAME_LENGTH)
        if (name.isEmpty()) return
        viewModelScope.launch { settingsRepository.setUserName(name) }
    }

    fun onConfirmBeforeApplyChange(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setConfirmBeforeApply(enabled) }
    }

    /** Dipanggil saat layar aktif kembali, mis. setelah user memberi akses di Settings sistem. */
    fun refreshUsageAccess() {
        usageAccess.value = gameRepository.hasUsageAccess()
    }

    companion object {
        const val MAX_NAME_LENGTH = 24
    }
}
