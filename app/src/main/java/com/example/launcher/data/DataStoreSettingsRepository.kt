package com.example.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.launcher.domain.model.AppSettings
import com.example.launcher.domain.repository.SettingsRepository
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
            )
        }

    override suspend fun setUserName(name: String) {
        context.settingsDataStore.edit { it[USER_NAME] = name }
    }

    override suspend fun setConfirmBeforeApply(enabled: Boolean) {
        context.settingsDataStore.edit { it[CONFIRM_BEFORE_APPLY] = enabled }
    }

    private companion object {
        val USER_NAME = stringPreferencesKey("user_name")
        val CONFIRM_BEFORE_APPLY = booleanPreferencesKey("confirm_before_apply")
    }
}
