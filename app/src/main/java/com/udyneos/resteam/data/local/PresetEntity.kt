package com.udyneos.resteam.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.udyneos.resteam.domain.model.CompileFilter
import com.udyneos.resteam.domain.model.GameMode
import com.udyneos.resteam.domain.model.TweakPreset

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
