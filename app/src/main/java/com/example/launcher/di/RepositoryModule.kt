package com.example.launcher.di

import com.example.launcher.data.InstalledGameRepository
import com.example.launcher.data.RoomPresetRepository
import com.example.launcher.domain.repository.GameRepository
import com.example.launcher.domain.repository.PresetRepository
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
}
