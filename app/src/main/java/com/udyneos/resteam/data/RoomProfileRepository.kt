package com.udyneos.resteam.data

import com.udyneos.resteam.data.local.GameProfileDao
import com.udyneos.resteam.data.local.toDomain
import com.udyneos.resteam.data.local.toEntity
import com.udyneos.resteam.domain.model.GameProfile
import com.udyneos.resteam.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomProfileRepository @Inject constructor(
    private val dao: GameProfileDao,
) : ProfileRepository {

    override fun observeProfiles(): Flow<Map<String, GameProfile>> =
        dao.observeAll().map { list -> list.associate { it.gameId to it.toDomain() } }

    override suspend fun getProfile(gameId: String): GameProfile? = dao.get(gameId)?.toDomain()

    override suspend fun save(profile: GameProfile) = dao.upsert(profile.toEntity())
}
