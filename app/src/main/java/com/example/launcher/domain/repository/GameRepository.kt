package com.example.launcher.domain.repository

import com.example.launcher.domain.model.Game

interface GameRepository {
    /** Game terpasang, diurutkan dari yang terakhir dimainkan. */
    suspend fun getGames(): List<Game>

    /** Jam bermain per hari bulan ini; `null` = sel kosong (sebelum tanggal 1 atau hari yang belum terjadi). Grid mulai Senin. */
    suspend fun getMonthlyActivity(packageName: String): List<Float?>

    fun hasUsageAccess(): Boolean
}
