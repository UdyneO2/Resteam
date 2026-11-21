package com.udyneos.resteam.domain.repository

import com.udyneos.resteam.domain.model.InfoRow

interface GameInfoRepository {
    fun getDeviceInfo(): List<InfoRow>
    fun getAppInfo(packageName: String): List<InfoRow>
}
