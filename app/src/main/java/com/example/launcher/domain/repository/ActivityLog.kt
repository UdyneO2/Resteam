package com.example.launcher.domain.repository

import com.example.launcher.domain.model.LogEntry
import kotlinx.coroutines.flow.StateFlow

/** Catatan aktivitas (mis. hasil menerapkan preset) yang dibagi ke seluruh layar. */
interface ActivityLog {
    val entries: StateFlow<List<LogEntry>>
    fun record(title: String, detail: String, isError: Boolean = false)
    fun clear()
}
