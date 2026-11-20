package com.example.launcher.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GameOverrideDao {
    @Query("SELECT * FROM game_overrides")
    suspend fun getAll(): List<GameOverrideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: GameOverrideEntity)

    @Query("DELETE FROM game_overrides WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}
