package com.example.launcher.data

import com.example.launcher.data.local.PresetDao
import com.example.launcher.data.local.PresetEntity
import com.example.launcher.data.local.toDomain
import com.example.launcher.domain.model.CompileFilter
import com.example.launcher.domain.model.GameMode
import com.example.launcher.domain.model.TweakPreset
import com.example.launcher.domain.repository.PresetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomPresetRepository @Inject constructor(
    private val dao: PresetDao,
) : PresetRepository {

    override fun observePresets(gameId: String): Flow<List<TweakPreset>> =
        dao.observeByGame(gameId).map { list -> list.map { it.toDomain() } }

    override suspend fun seedDefaultsIfEmpty(gameId: String) {
        if (dao.countByGame(gameId) > 0) return
        dao.insertAll(
            listOf(
                PresetEntity(gameId = gameId, name = "Performance", gameMode = GameMode.Performance.arg, compileFilter = CompileFilter.SpeedProfile.arg),
                PresetEntity(gameId = gameId, name = "Balanced", gameMode = GameMode.Standard.arg, compileFilter = null),
                PresetEntity(gameId = gameId, name = "Battery Saver", gameMode = GameMode.Battery.arg, compileFilter = null),
            ),
        )
    }
}
