package com.udyneos.glasslauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    private val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps

    // Fires whenever something is installed, removed or updated.
    var onPackagesChanged: (() -> Unit)? = null

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) = notifyChanged()
        override fun onPackageChanged(packageName: String, user: UserHandle) = notifyChanged()
        override fun onPackageRemoved(packageName: String, user: UserHandle) = notifyChanged()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = notifyChanged()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = notifyChanged()
    }

    private fun notifyChanged() {
        onPackagesChanged?.invoke()
    }

    fun startWatching() = launcherApps.registerCallback(callback)

    fun stopWatching() = launcherApps.unregisterCallback(callback)

    suspend fun refresh() {
        val loaded = withContext(Dispatchers.Default) {
            launcherApps.getActivityList(null, Process.myUserHandle())
                .filter { it.applicationInfo.packageName != context.packageName }
                .map { info ->
                    val bitmap = info.getIcon(0).toBitmap(width = 144, height = 144)
                    AppInfo(
                        label = info.label.toString(),
                        packageName = info.applicationInfo.packageName,
                        activityName = info.componentName.className,
                        category = info.applicationInfo.category,
                        icon = bitmap.asImageBitmap(),
                    )
                }
                .sortedBy { it.label.lowercase() }
        }
        _apps.value = loaded
    }

    fun launch(app: AppInfo) {
        runCatching {
            launcherApps.startMainActivity(
                ComponentName(app.packageName, app.activityName),
                Process.myUserHandle(),
                null,
                null,
            )
        }
    }

    fun openAppInfo(app: AppInfo) {
        runCatching {
            launcherApps.startAppDetailsActivity(
                ComponentName(app.packageName, app.activityName),
                Process.myUserHandle(),
                null,
                null,
            )
        }
    }

    fun uninstall(app: AppInfo) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = android.net.Uri.parse("package:${app.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    companion object {
        fun categoryOf(app: AppInfo): AppCategory = when (app.category) {
            ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.Social
            ApplicationInfo.CATEGORY_GAME -> AppCategory.Games
            ApplicationInfo.CATEGORY_AUDIO,
            ApplicationInfo.CATEGORY_VIDEO,
            ApplicationInfo.CATEGORY_IMAGE -> AppCategory.Media
            ApplicationInfo.CATEGORY_PRODUCTIVITY,
            ApplicationInfo.CATEGORY_NEWS -> AppCategory.Productivity
            else -> AppCategory.Utilities
        }
    }
}

enum class AppCategory(val title: String) {
    Social("Social"),
    Media("Media"),
    Productivity("Productivity"),
    Games("Games"),
    Utilities("Utilities"),
}
