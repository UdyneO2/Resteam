package com.udyneos.resteam.data.local

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
        ScriptEntity::class,
        PendingRestoreEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao
    abstract fun gameOverrideDao(): GameOverrideDao
    abstract fun gameProfileDao(): GameProfileDao
    abstract fun scriptDao(): ScriptDao

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

        /** Tabel script global (dari GitHub) dan antrean restore; data lama tidak disentuh. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `scripts` (`fileName` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                        "`description` TEXT NOT NULL, `version` TEXT NOT NULL, `content` TEXT NOT NULL, " +
                        "`sha` TEXT NOT NULL, `enabled` INTEGER NOT NULL, PRIMARY KEY(`fileName`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pending_restores` (`fileName` TEXT NOT NULL, " +
                        "`gamePackage` TEXT NOT NULL, `appliedAt` INTEGER NOT NULL, PRIMARY KEY(`fileName`))",
                )
            }
        }
    }
}
