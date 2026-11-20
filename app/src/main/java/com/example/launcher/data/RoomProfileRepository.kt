package com.example.launcher.data

import com.example.launcher.data.local.GameProfileDao
import com.example.launcher.data.local.toDomain
import com.example.launcher.data.local.toEntity
import com.example.launcher.domain.model.GameProfile
import com.example.launcher.domain.repository.ProfileRepository
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
