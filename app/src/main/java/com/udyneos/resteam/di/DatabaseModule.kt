package com.udyneos.resteam.di

import android.content.Context
import androidx.room.Room
import com.udyneos.resteam.data.local.AppDatabase
import com.udyneos.resteam.data.local.GameOverrideDao
import com.udyneos.resteam.data.local.GameProfileDao
import com.udyneos.resteam.data.local.PresetDao
import com.udyneos.resteam.data.local.ScriptDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "launcher.db")
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
            )
            .build()

    @Provides
    fun providePresetDao(db: AppDatabase): PresetDao = db.presetDao()

    @Provides
    fun provideGameOverrideDao(db: AppDatabase): GameOverrideDao = db.gameOverrideDao()

    @Provides
    fun provideGameProfileDao(db: AppDatabase): GameProfileDao = db.gameProfileDao()

    @Provides
    fun provideScriptDao(db: AppDatabase): ScriptDao = db.scriptDao()
}
