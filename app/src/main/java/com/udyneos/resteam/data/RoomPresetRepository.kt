package com.udyneos.resteam.data

import com.udyneos.resteam.data.local.PresetDao
import com.udyneos.resteam.data.local.PresetEntity
import com.udyneos.resteam.data.local.PresetSeedEntity
import com.udyneos.resteam.data.local.toDomain
import com.udyneos.resteam.data.local.toEntity
import com.udyneos.resteam.domain.model.CompileFilter
import com.udyneos.resteam.domain.model.GameMode
import com.udyneos.resteam.domain.model.TweakPreset
import com.udyneos.resteam.domain.repository.PresetRepository
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
