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
