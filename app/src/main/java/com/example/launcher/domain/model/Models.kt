package com.example.launcher.domain.model

/** Game terdeteksi dari PackageManager. `id` = packageName. */
data class Game(
    val id: String,
    val title: String,
    val packageName: String,
    /** Epoch millis; null jika belum pernah dimainkan atau akses penggunaan belum diberikan. */
    val lastPlayedAt: Long? = null,
    /** Null jika akses penggunaan belum diberikan. */
    val hoursPlayed: Int? = null,
    val coverUrl: String? = null,
)

enum class ActivityLevel {
    None, Low, Medium, High;

    companion object {
        fun fromHours(hours: Float): ActivityLevel = when {
            hours > 8f -> High
            hours > 4f -> Medium
            hours > 2f -> Low
            else -> None
        }
    }
}

enum class LauncherTab(val label: String) {
    AllGames("All Games"),
    ShizukuTweaks("Shizuku Tweaks"),
    Profiles("Profiles"),
    Logs("Logs"),
}

enum class ShizukuMode { Adb, Root }

sealed interface ShizukuState {
    data object Unavailable : ShizukuState
    data object PermissionRequired : ShizukuState
    data object PermissionDenied : ShizukuState
    data class Ready(val mode: ShizukuMode) : ShizukuState
    data class Error(val message: String) : ShizukuState
}

/** Aplikasi terpasang yang bisa dimasukkan/dikeluarkan dari daftar game. */
data class ManagedApp(
    val packageName: String,
    val title: String,
    /** Status efektif: override user, atau hasil deteksi otomatis jika belum di-override. */
    val isGame: Boolean,
    val detectedAsGame: Boolean,
)
