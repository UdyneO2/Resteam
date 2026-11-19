package com.example.launcher.domain.repository

import com.example.launcher.domain.model.TweakPreset
import kotlinx.coroutines.flow.Flow

interface PresetRepository {
    fun observePresets(gameId: String): Flow<List<TweakPreset>>
    suspend fun seedDefaultsIfEmpty(gameId: String)
}
