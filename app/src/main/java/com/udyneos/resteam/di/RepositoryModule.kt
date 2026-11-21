package com.udyneos.resteam.di

import com.udyneos.resteam.data.AndroidGameInfoRepository
import com.udyneos.resteam.data.DataStoreSettingsRepository
import com.udyneos.resteam.data.GitHubScriptSource
import com.udyneos.resteam.data.InMemoryActivityLog
import com.udyneos.resteam.data.InstalledGameRepository
import com.udyneos.resteam.data.RoomPresetRepository
import com.udyneos.resteam.data.RoomProfileRepository
import com.udyneos.resteam.data.RoomScriptRepository
import com.udyneos.resteam.domain.repository.ActivityLog
import com.udyneos.resteam.domain.repository.GameInfoRepository
import com.udyneos.resteam.domain.repository.GameRepository
import com.udyneos.resteam.domain.repository.PresetRepository
import com.udyneos.resteam.domain.repository.ProfileRepository
import com.udyneos.resteam.domain.repository.RemoteScriptSource
import com.udyneos.resteam.domain.repository.ScriptRepository
import com.udyneos.resteam.domain.repository.SettingsRepository
import com.udyneos.resteam.domain.repository.ShellExecutor
import com.udyneos.resteam.shizuku.ShizukuShellExecutor
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
    @Singleton
    abstract fun bindScriptRepository(impl: RoomScriptRepository): ScriptRepository

    @Binds
    @Singleton
    abstract fun bindRemoteScriptSource(impl: GitHubScriptSource): RemoteScriptSource

    @Binds
    abstract fun bindGameInfoRepository(impl: AndroidGameInfoRepository): GameInfoRepository
}
