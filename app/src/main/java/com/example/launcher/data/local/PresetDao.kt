package com.example.launcher.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets WHERE gameId = :gameId ORDER BY id")
    fun observeByGame(gameId: String): Flow<List<PresetEntity>>

    @Query("SELECT COUNT(*) FROM presets WHERE gameId = :gameId")
    suspend fun countByGame(gameId: String): Int

    @Insert
    suspend fun insertAll(presets: List<PresetEntity>)
}
