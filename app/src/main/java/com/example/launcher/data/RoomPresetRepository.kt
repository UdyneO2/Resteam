package com.example.launcher.data

import com.example.launcher.data.local.PresetDao
import com.example.launcher.data.local.PresetEntity
import com.example.launcher.data.local.PresetSeedEntity
import com.example.launcher.data.local.toDomain
import com.example.launcher.data.local.toEntity
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

    override fun observeAllPresets(): Flow<List<TweakPreset>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun seedDefaultsOnce(gameId: String) {
        if (dao.seedCount(gameId) > 0) return
        if (dao.countByGame(gameId) == 0) dao.insertAll(
            listOf(
                PresetEntity(gameId = gameId, name = "Performance", gameMode = GameMode.Performance.arg, compileFilter = CompileFilter.SpeedProfile.arg),
                PresetEntity(gameId = gameId, name = "Balanced", gameMode = GameMode.Standard.arg, compileFilter = null),
                PresetEntity(gameId = gameId, name = "Battery Saver", gameMode = GameMode.Battery.arg, compileFilter = null),
            ),
        )
        dao.markSeeded(PresetSeedEntity(gameId))
    }

    override suspend fun save(preset: TweakPreset) {
        if (preset.id == 0L) dao.insert(preset.toEntity()) else dao.update(preset.toEntity())
    }

    override suspend fun delete(id: Long) = dao.delete(id)
}
