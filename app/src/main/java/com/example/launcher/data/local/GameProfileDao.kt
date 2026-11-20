package com.example.launcher.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameProfileDao {
    @Query("SELECT * FROM game_profiles")
    fun observeAll(): Flow<List<GameProfileEntity>>

    @Query("SELECT * FROM game_profiles WHERE gameId = :gameId")
    suspend fun get(gameId: String): GameProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: GameProfileEntity)
}
