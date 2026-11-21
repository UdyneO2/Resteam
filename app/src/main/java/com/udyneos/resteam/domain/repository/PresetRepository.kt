package com.udyneos.resteam.domain.repository

import com.udyneos.resteam.domain.model.TweakPreset
import kotlinx.coroutines.flow.Flow

interface PresetRepository {
    fun observePresets(gameId: String): Flow<List<TweakPreset>>
    fun observeAllPresets(): Flow<List<TweakPreset>>
    suspend fun seedDefaultsOnce(gameId: String)

    /** `id == 0` membuat preset baru, selain itu memperbarui preset yang ada. */
    suspend fun save(preset: TweakPreset)
    suspend fun delete(id: Long)
}
