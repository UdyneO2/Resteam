package com.udyneos.resteam.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.udyneos.resteam.domain.model.AppSettings
import com.udyneos.resteam.domain.repository.SettingsRepository
import com.udyneos.resteam.domain.usecase.isValidBranch
import com.udyneos.resteam.domain.usecase.isValidRepo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = context.settingsDataStore.data
        // File rusak/tidak terbaca: pakai nilai default, jangan crash
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs ->
            AppSettings(
                userName = prefs[USER_NAME] ?: AppSettings.DEFAULT_NAME,
                confirmBeforeApply = prefs[CONFIRM_BEFORE_APPLY] ?: true,
                shizukuTweaksEnabled = prefs[SHIZUKU_TWEAKS_ENABLED] ?: false,
                scriptRepo = prefs[SCRIPT_REPO] ?: AppSettings.DEFAULT_SCRIPT_REPO,
                scriptBranch = prefs[SCRIPT_BRANCH] ?: AppSettings.DEFAULT_SCRIPT_BRANCH,
                scriptsLastUpdatedAt = prefs[SCRIPTS_LAST_UPDATED] ?: 0L,
            )
        }

    override suspend fun setUserName(name: String) {
        context.settingsDataStore.edit { it[USER_NAME] = name }
    }

    override suspend fun setConfirmBeforeApply(enabled: Boolean) {
        context.settingsDataStore.edit { it[CONFIRM_BEFORE_APPLY] = enabled }
    }

    override suspend fun setShizukuTweaksEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[SHIZUKU_TWEAKS_ENABLED] = enabled }
    }

    override suspend fun setScriptSource(repo: String, branch: String) {
        require(isValidRepo(repo) && isValidBranch(branch)) { "Repo atau branch tidak valid" }
        context.settingsDataStore.edit {
            it[SCRIPT_REPO] = repo
            it[SCRIPT_BRANCH] = branch
        }
    }

    override suspend fun setScriptBranch(branch: String) {
        require(isValidBranch(branch)) { "Nama branch tidak valid" }
        context.settingsDataStore.edit { it[SCRIPT_BRANCH] = branch }
    }

    override suspend fun setScriptsLastUpdatedAt(epochMillis: Long) {
        context.settingsDataStore.edit { it[SCRIPTS_LAST_UPDATED] = epochMillis }
    }

    private companion object {
        val USER_NAME = stringPreferencesKey("user_name")
        val CONFIRM_BEFORE_APPLY = booleanPreferencesKey("confirm_before_apply")
        val SHIZUKU_TWEAKS_ENABLED = booleanPreferencesKey("shizuku_tweaks_enabled")
        val SCRIPT_REPO = stringPreferencesKey("script_repo")
        val SCRIPT_BRANCH = stringPreferencesKey("script_branch")
        val SCRIPTS_LAST_UPDATED = longPreferencesKey("scripts_last_updated")
    }
}
