package com.example.launcher.domain.model

data class LogEntry(
    val timestamp: Long,
    val title: String,
    val detail: String,
    val isError: Boolean,
)

data class InfoRow(
    val label: String,
    val value: String,
)
