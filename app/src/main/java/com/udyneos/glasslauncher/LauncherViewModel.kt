package com.udyneos.glasslauncher

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.udyneos.glasslauncher.data.AppInfo
import com.udyneos.glasslauncher.data.AppRepository
import com.udyneos.glasslauncher.data.LauncherPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    val repository = AppRepository(app)
    val prefs = LauncherPrefs(app)

    val apps: StateFlow<List<AppInfo>> = repository.apps

    /** Dock = whatever handles phone, messages, browser and camera on this device. */
    val dock: StateFlow<List<AppInfo>> = repository.apps
        .map { list -> pickDock(list) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        repository.onPackagesChanged = { reload() }
        repository.startWatching()
        reload()
    }

    fun reload() {
        viewModelScope.launch { repository.refresh() }
    }

    private fun pickDock(all: List<AppInfo>): List<AppInfo> {
        val pm = getApplication<Application>().packageManager
        val probes = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")),
            Intent(MediaStore.ACTION_IMAGE_CAPTURE),
        )
        return probes.mapNotNull { probe ->
            val pkg = pm.resolveActivity(probe, 0)?.activityInfo?.packageName
            all.firstOrNull { it.packageName == pkg }
        }.distinctBy { it.key }
    }

    override fun onCleared() {
        repository.stopWatching()
    }
}
