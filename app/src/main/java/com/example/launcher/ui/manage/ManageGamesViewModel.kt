package com.example.launcher.ui.manage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.launcher.domain.model.ManagedApp
import com.example.launcher.domain.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ManageUiState(
    val apps: List<ManagedApp> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class ManageGamesViewModel @Inject constructor(
    private val repository: GameRepository,
) : ViewModel() {

    private val allApps = MutableStateFlow<List<ManagedApp>>(emptyList())
    private val query = MutableStateFlow("")
    private val isLoading = MutableStateFlow(true)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ManageUiState> = combine(allApps, query, isLoading, error) { apps, q, loading, err ->
        val filtered = if (q.isBlank()) apps else apps.filter {
            it.title.contains(q, ignoreCase = true) || it.packageName.contains(q, ignoreCase = true)
        }
        ManageUiState(apps = filtered, query = q, isLoading = loading, error = err)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ManageUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            runCatching { repository.getInstalledApps() }
                .onSuccess { allApps.value = it }
                .onFailure {
                    if (it is CancellationException) throw it
                    error.value = it.message ?: "Gagal memuat daftar aplikasi"
                }
            isLoading.value = false
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onToggle(app: ManagedApp, included: Boolean) {
        viewModelScope.launch {
            runCatching { repository.setGameIncluded(app.packageName, included) }
                .onSuccess {
                    // Update lokal agar baris tidak melompat; urutan dirapikan saat refresh berikutnya
                    allApps.update { list ->
                        list.map { if (it.packageName == app.packageName) it.copy(isGame = included) else it }
                    }
                }
                .onFailure {
                    if (it is CancellationException) throw it
                    error.value = it.message ?: "Gagal menyimpan pilihan"
                }
        }
    }
}
