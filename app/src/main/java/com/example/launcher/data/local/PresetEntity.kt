package com.example.launcher.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.launcher.domain.model.CompileFilter
import com.example.launcher.domain.model.GameMode
import com.example.launcher.domain.model.TweakPreset

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: String,
    val name: String,
    val gameMode: String,
    val compileFilter: String?,
)

fun PresetEntity.toDomain() = TweakPreset(
    id = id,
    gameId = gameId,
    name = name,
    gameMode = GameMode.fromArg(gameMode),
    compileFilter = CompileFilter.fromArg(compileFilter),
)

fun TweakPreset.toEntity() = PresetEntity(
    id = id,
    gameId = gameId,
    name = name,
    gameMode = gameMode.arg,
    compileFilter = compileFilter?.arg,
)
