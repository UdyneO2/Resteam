package com.example.launcher.di

import com.example.launcher.data.AndroidGameInfoRepository
import com.example.launcher.data.DataStoreSettingsRepository
import com.example.launcher.data.InMemoryActivityLog
import com.example.launcher.data.InstalledGameRepository
import com.example.launcher.data.RoomPresetRepository
import com.example.launcher.data.RoomProfileRepository
import com.example.launcher.domain.repository.ActivityLog
import com.example.launcher.domain.repository.GameInfoRepository
import com.example.launcher.domain.repository.GameRepository
import com.example.launcher.domain.repository.PresetRepository
import com.example.launcher.domain.repository.ProfileRepository
import com.example.launcher.domain.repository.SettingsRepository
import com.example.launcher.domain.repository.ShellExecutor
import com.example.launcher.shizuku.ShizukuShellExecutor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindGameRepository(impl: InstalledGameRepository): GameRepository

    @Binds
    @Singleton
    abstract fun bindPresetRepository(impl: RoomPresetRepository): PresetRepository

    @Binds
    @Singleton
    abstract fun bindShellExecutor(impl: ShizukuShellExecutor): ShellExecutor

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: RoomProfileRepository): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindActivityLog(impl: InMemoryActivityLog): ActivityLog

    @Binds
    abstract fun bindGameInfoRepository(impl: AndroidGameInfoRepository): GameInfoRepository
}
