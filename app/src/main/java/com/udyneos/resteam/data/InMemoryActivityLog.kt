package com.udyneos.resteam.data

import com.udyneos.resteam.domain.model.LogEntry
import com.udyneos.resteam.domain.repository.ActivityLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** Disimpan di memori: hilang saat proses aplikasi dimatikan. */
@Singleton
class InMemoryActivityLog @Inject constructor() : ActivityLog {

    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    override val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    override fun record(title: String, detail: String, isError: Boolean) {
        val entry = LogEntry(System.currentTimeMillis(), title, detail, isError)
        _entries.update { (it + entry).takeLast(MAX_ENTRIES) }
    }

    override fun clear() {
        _entries.value = emptyList()
    }

    private companion object {
        const val MAX_ENTRIES = 200
    }
}
