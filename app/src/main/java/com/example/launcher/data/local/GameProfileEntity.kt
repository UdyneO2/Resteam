package com.example.launcher.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.launcher.domain.model.GameProfile

@Entity(tableName = "game_profiles")
data class GameProfileEntity(
    @PrimaryKey val gameId: String,
    val presetId: Long?,
    val autoApply: Boolean,
)

fun GameProfileEntity.toDomain() = GameProfile(gameId, presetId, autoApply)
fun GameProfile.toEntity() = GameProfileEntity(gameId, presetId, autoApply)
