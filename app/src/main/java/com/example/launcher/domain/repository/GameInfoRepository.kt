package com.example.launcher.domain.repository

import com.example.launcher.domain.model.InfoRow

interface GameInfoRepository {
    fun getDeviceInfo(): List<InfoRow>
    fun getAppInfo(packageName: String): List<InfoRow>
}
