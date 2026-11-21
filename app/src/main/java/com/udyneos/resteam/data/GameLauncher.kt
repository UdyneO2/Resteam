package com.udyneos.resteam.data

import android.content.Context
import android.content.Intent
import com.udyneos.resteam.session.GameSessionService
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
        // Script Shizuku (jika diaktifkan) berjalan di service; kegagalan di sini tidak boleh menghalangi game
        GameSessionService.start(context, packageName)
    }
}
