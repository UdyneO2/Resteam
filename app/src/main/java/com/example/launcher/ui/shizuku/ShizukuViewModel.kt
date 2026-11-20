package com.example.launcher.ui.shizuku

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.launcher.domain.model.ShizukuState
import com.example.launcher.domain.repository.ShellExecutor
import com.example.launcher.shizuku.ShizukuManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DiagnosticState {
    data object Idle : DiagnosticState
    data object Running : DiagnosticState
    data class Finished(val output: String, val success: Boolean) : DiagnosticState
}

data class ShizukuUiState(
    val shizuku: ShizukuState = ShizukuState.Unavailable,
    val diagnostic: DiagnosticState = DiagnosticState.Idle,
)

@HiltViewModel
class ShizukuViewModel @Inject constructor(
    private val manager: ShizukuManager,
    private val shell: ShellExecutor,
) : ViewModel() {

    private val shizukuState = MutableStateFlow<ShizukuState>(ShizukuState.Unavailable)
    private val diagnostic = MutableStateFlow<DiagnosticState>(DiagnosticState.Idle)

    val uiState: StateFlow<ShizukuUiState> = combine(shizukuState, diagnostic) { s, d ->
        ShizukuUiState(shizuku = s, diagnostic = d)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShizukuUiState())

    init {
        viewModelScope.launch {
            manager.state.collect { state ->
                shizukuState.value = state
                // Hasil diagnostik lama tidak relevan jika Shizuku berhenti
                if (state !is ShizukuState.Ready) diagnostic.value = DiagnosticState.Idle
            }
        }
    }

    fun onRequestPermission() = manager.requestPermission()

    fun onRunDiagnostic() {
        if (shizukuState.value !is ShizukuState.Ready || diagnostic.value is DiagnosticState.Running) return
        viewModelScope.launch {
            diagnostic.value = DiagnosticState.Running
            diagnostic.value = runDiagnostic()
        }
    }

    private suspend fun runDiagnostic(): DiagnosticState {
        val sections = mutableListOf<String>()
        for (command in DIAGNOSTIC_COMMANDS) {
            val result = shell.run(command).getOrElse { error ->
                return DiagnosticState.Finished(error.message ?: "Gagal menjalankan perintah", success = false)
            }
            sections += "$ ${result.command}\n${result.output.ifBlank { "(kosong)" }}"
            if (!result.isSuccess) return DiagnosticState.Finished(sections.joinToString("\n\n"), success = false)
        }
        return DiagnosticState.Finished(sections.joinToString("\n\n"), success = true)
    }

    private companion object {
        // Read-only; aman dijalankan untuk memverifikasi koneksi
        val DIAGNOSTIC_COMMANDS = listOf(
            listOf("id"),
            listOf("getprop", "ro.build.version.release"),
        )
    }
}
