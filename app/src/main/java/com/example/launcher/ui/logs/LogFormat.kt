package com.example.launcher.ui.logs

import com.example.launcher.domain.model.LogEntry
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

fun LogEntry.formattedTime(): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(TIME_FORMAT)

fun LogEntry.toDisplayText(): String = "[${formattedTime()}] $title\n$detail"
