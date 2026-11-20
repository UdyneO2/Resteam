package com.example.launcher.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PresetEntity::class,
        GameOverrideEntity::class,
        PresetSeedEntity::class,
        GameProfileEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao
    abstract fun gameOverrideDao(): GameOverrideDao
    abstract fun gameProfileDao(): GameProfileDao

    companion object {
        /** Menambah tabel override tanpa menghapus preset yang sudah ada. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `game_overrides` " +
                        "(`packageName` TEXT NOT NULL, `included` INTEGER NOT NULL, PRIMARY KEY(`packageName`))",
                )
            }
        }

        /** Menandai game yang sudah punya preset sebagai "sudah di-seed" agar tidak di-seed ulang. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `preset_seeds` (`gameId` TEXT NOT NULL, PRIMARY KEY(`gameId`))")
                db.execSQL("INSERT OR IGNORE INTO preset_seeds (gameId) SELECT DISTINCT gameId FROM presets")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `game_profiles` " +
                        "(`gameId` TEXT NOT NULL, `presetId` INTEGER, `autoApply` INTEGER NOT NULL, PRIMARY KEY(`gameId`))",
                )
            }
        }
    }
}
