package com.example.launcher.di

import android.content.Context
import androidx.room.Room
import com.example.launcher.data.local.AppDatabase
import com.example.launcher.data.local.PresetDao
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
        Room.databaseBuilder(context, AppDatabase::class.java, "launcher.db").build()

    @Provides
    fun providePresetDao(db: AppDatabase): PresetDao = db.presetDao()
}
