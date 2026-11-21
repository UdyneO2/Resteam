package com.udyneos.resteam.ui.scripts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udyneos.resteam.domain.model.AppSettings
import com.udyneos.resteam.domain.model.ShizukuScript
import com.udyneos.resteam.domain.repository.RemoteScriptSource
import com.udyneos.resteam.domain.repository.ScriptRepository
import com.udyneos.resteam.domain.repository.SettingsRepository
import com.udyneos.resteam.domain.usecase.UpdateScriptsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScriptsUiState(
    val scripts: List<ShizukuScript> = emptyList(),
    val branch: String = AppSettings.DEFAULT_SCRIPT_BRANCH,
    val repo: String = AppSettings.DEFAULT_SCRIPT_REPO,
    val branches: List<String> = emptyList(),
    val lastUpdatedAt: Long = 0L,
    val isUpdating: Boolean = false,
)

sealed interface ScriptsEvent {
    data class ShowMessage(val text: String) : ScriptsEvent
}

@HiltViewModel
class ScriptsViewModel @Inject constructor(
    private val scripts: ScriptRepository,
    private val settings: SettingsRepository,
    private val updateScripts: UpdateScriptsUseCase,
    private val remote: RemoteScriptSource,
) : ViewModel() {

    private val isUpdating = MutableStateFlow(false)
    private val branches = MutableStateFlow<List<String>>(emptyList())
    private val _events = Channel<ScriptsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val uiState: StateFlow<ScriptsUiState> = combine(
        scripts.observeScripts(),
        settings.settings,
        branches,
        isUpdating,
    ) { list, config, branchList, updating ->
        ScriptsUiState(
            scripts = list,
            branch = config.scriptBranch,
            repo = config.scriptRepo,
            // Branch aktif selalu ada di daftar walau fetch branch belum/gagal
            branches = (listOf(config.scriptBranch) + branchList).distinct(),
            lastUpdatedAt = config.scriptsLastUpdatedAt,
            isUpdating = updating,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScriptsUiState())

    fun onToggle(script: ShizukuScript, enabled: Boolean) {
        viewModelScope.launch { scripts.setEnabled(script.fileName, enabled) }
    }

    /** Dipanggil manual lewat tombol Update; tidak ada update otomatis. */
    fun onUpdate() {
        if (!isUpdating.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            try {
                updateScripts().fold(
                    onSuccess = { summary ->
                        send(
                            if (summary.hasChanges) {
                                "Diperbarui: ${summary.added} baru, ${summary.updated} diubah, ${summary.removed} dihapus"
                            } else {
                                "Script sudah terbaru"
                            },
                        )
                    },
                    onFailure = { send(it.message ?: "Gagal memperbarui script") },
                )
            } finally {
                isUpdating.value = false
            }
        }
    }

    fun onBranchSelected(branch: String) {
        if (branch == uiState.value.branch) return
        viewModelScope.launch {
            runCatching { settings.setScriptBranch(branch) }
                .onSuccess { onUpdate() }
                .onFailure { send(it.message ?: "Branch tidak valid") }
        }
    }

    /** Mengambil daftar branch untuk dropdown; gagal diam-diam, branch aktif tetap tampil. */
    fun loadBranches() {
        viewModelScope.launch {
            remote.listBranches(uiState.value.repo).onSuccess { list -> branches.update { list } }
        }
    }

    private fun send(text: String) {
        _events.trySend(ScriptsEvent.ShowMessage(text))
    }
}

/** Gate global: script aktif hanya dijalankan saat Launch jika toggle ini menyala. */
@HiltViewModel
class ScriptsGateViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {
    val enabled: StateFlow<Boolean> = settings.settings.map { it.shizukuTweaksEnabled }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun onChange(value: Boolean) {
        viewModelScope.launch { settings.setShizukuTweaksEnabled(value) }
    }
}
