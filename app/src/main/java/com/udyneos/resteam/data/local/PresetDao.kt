package com.udyneos.resteam.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets WHERE gameId = :gameId ORDER BY id")
    fun observeByGame(gameId: String): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets ORDER BY id")
    fun observeAll(): Flow<List<PresetEntity>>

    @Query("SELECT COUNT(*) FROM presets WHERE gameId = :gameId")
    suspend fun countByGame(gameId: String): Int

    @Insert
    suspend fun insertAll(presets: List<PresetEntity>)

    @Insert
    suspend fun insert(preset: PresetEntity)

    @Update
    suspend fun update(preset: PresetEntity)

    @Query("DELETE FROM presets WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM preset_seeds WHERE gameId = :gameId")
    suspend fun seedCount(gameId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markSeeded(seed: PresetSeedEntity)
}
