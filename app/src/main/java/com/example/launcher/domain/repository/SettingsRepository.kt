package com.example.launcher.domain.repository

import com.example.launcher.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setUserName(name: String)
    suspend fun setConfirmBeforeApply(enabled: Boolean)
}
