package com.example.launcher.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PresetEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao
}
