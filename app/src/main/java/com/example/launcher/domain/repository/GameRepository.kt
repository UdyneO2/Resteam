package com.example.launcher.domain.repository

import com.example.launcher.domain.model.Game
import com.example.launcher.domain.model.ManagedApp

interface GameRepository {
    /** Game terpasang, diurutkan dari yang terakhir dimainkan. */
    suspend fun getGames(): List<Game>

    /** Jam bermain per hari bulan ini; `null` = sel kosong (sebelum tanggal 1 atau hari yang belum terjadi). Grid mulai Senin. */
    suspend fun getMonthlyActivity(packageName: String): List<Float?>

    fun hasUsageAccess(): Boolean

    /** Semua aplikasi launcher terpasang; game di urutan atas. */
    suspend fun getInstalledApps(): List<ManagedApp>

    /** Masukkan/keluarkan aplikasi dari daftar game. Override dihapus jika sama dengan hasil deteksi otomatis. */
    suspend fun setGameIncluded(packageName: String, included: Boolean)
}
