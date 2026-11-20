package com.example.launcher.domain.repository

import com.example.launcher.domain.model.GameProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    /** Profil berdasarkan gameId. */
    fun observeProfiles(): Flow<Map<String, GameProfile>>
    suspend fun getProfile(gameId: String): GameProfile?
    suspend fun save(profile: GameProfile)
}
