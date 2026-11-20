package com.example.launcher.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.launcher.domain.model.LogEntry
import com.example.launcher.domain.repository.ActivityLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val activityLog: ActivityLog,
) : ViewModel() {

    /** Terbaru di atas. */
    val entries: StateFlow<List<LogEntry>> = activityLog.entries
        .map { it.asReversed() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onClear() = activityLog.clear()
}
