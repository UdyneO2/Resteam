package com.udyneos.resteam.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udyneos.resteam.domain.model.LogEntry
import com.udyneos.resteam.domain.repository.ActivityLog
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
