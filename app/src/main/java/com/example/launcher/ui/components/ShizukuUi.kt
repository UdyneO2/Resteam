package com.example.launcher.ui.components

import androidx.compose.ui.graphics.Color
import com.example.launcher.domain.model.ShizukuMode
import com.example.launcher.domain.model.ShizukuState
import com.example.launcher.ui.theme.LauncherColors

fun ShizukuState.pillText(): String = when (this) {
    is ShizukuState.Ready -> when (mode) {
        ShizukuMode.Adb -> "Shizuku Ready • ADB Active"
        ShizukuMode.Root -> "Shizuku Ready • Root Active"
    }
    ShizukuState.PermissionRequired -> "Shizuku • Izin diperlukan"
    ShizukuState.PermissionDenied -> "Shizuku • Izin ditolak"
    ShizukuState.Unavailable -> "Shizuku tidak aktif"
    is ShizukuState.Error -> "Shizuku error"
}

fun ShizukuState.indicatorColor(): Color = when (this) {
    is ShizukuState.Ready -> LauncherColors.Success
    ShizukuState.PermissionRequired, ShizukuState.PermissionDenied -> LauncherColors.Warning
    ShizukuState.Unavailable -> LauncherColors.TextSecondary
    is ShizukuState.Error -> LauncherColors.Error
}

fun ShizukuState.description(): String = when (this) {
    is ShizukuState.Ready -> when (mode) {
        ShizukuMode.Adb -> "Shizuku berjalan dengan hak akses ADB (shell). Preset tweak bisa diterapkan."
        ShizukuMode.Root -> "Shizuku berjalan dengan hak akses root. Preset tweak bisa diterapkan."
    }
    ShizukuState.PermissionRequired -> "Shizuku aktif, tetapi aplikasi ini belum diberi izin."
    ShizukuState.PermissionDenied ->
        "Izin ditolak. Buka aplikasi Shizuku lalu aktifkan aplikasi ini di daftar yang diizinkan."
    ShizukuState.Unavailable -> "Layanan Shizuku tidak berjalan. Pasang dan jalankan Shizuku terlebih dahulu."
    is ShizukuState.Error -> message
}
