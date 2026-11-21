package com.udyneos.resteam.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ScriptDao {
    @Query("SELECT * FROM scripts ORDER BY name COLLATE NOCASE")
    abstract fun observeAll(): Flow<List<ScriptEntity>>

    @Query("SELECT * FROM scripts ORDER BY name COLLATE NOCASE")
    abstract suspend fun getAll(): List<ScriptEntity>

    @Query("SELECT * FROM scripts WHERE enabled = 1 ORDER BY fileName")
    abstract suspend fun getEnabled(): List<ScriptEntity>

    @Query("SELECT * FROM scripts WHERE fileName = :fileName")
    abstract suspend fun get(fileName: String): ScriptEntity?

    @Query("UPDATE scripts SET enabled = :enabled WHERE fileName = :fileName")
    abstract suspend fun setEnabled(fileName: String, enabled: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertAll(entities: List<ScriptEntity>)

    @Query("DELETE FROM scripts WHERE fileName NOT IN (:keep)")
    abstract suspend fun deleteNotIn(keep: List<String>)

    @Query("DELETE FROM scripts")
    abstract suspend fun deleteAll()

    /**
     * Sinkron dengan hasil fetch: status toggle script yang sudah ada dipertahankan,
     * script yang hilang dari repo dihapus.
     */
    @Transaction
    open suspend fun sync(fetched: List<ScriptEntity>) {
        val enabledBefore = getAll().associate { it.fileName to it.enabled }
        if (fetched.isEmpty()) deleteAll() else deleteNotIn(fetched.map { it.fileName })
        upsertAll(fetched.map { it.copy(enabled = enabledBefore[it.fileName] ?: false) })
    }

    @Query("SELECT * FROM pending_restores")
    abstract suspend fun getPendingRestores(): List<PendingRestoreEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun addPendingRestore(entity: PendingRestoreEntity)

    @Query("DELETE FROM pending_restores WHERE fileName = :fileName")
    abstract suspend fun clearPendingRestore(fileName: String)
}
