package com.udyneos.resteam.domain.repository

import com.udyneos.resteam.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setUserName(name: String)
    suspend fun setConfirmBeforeApply(enabled: Boolean)
    suspend fun setShizukuTweaksEnabled(enabled: Boolean)
    suspend fun setScriptBranch(branch: String)
    suspend fun setScriptSource(repo: String, branch: String)
    suspend fun setScriptsLastUpdatedAt(epochMillis: Long)
}
