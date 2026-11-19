package com.example.launcher.data

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class GameLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun launch(packageName: String): Result<Unit> = runCatching {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?: error("Game belum terpasang: $packageName")
        context.startActivity(intent)
    }
}
