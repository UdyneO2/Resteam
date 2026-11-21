package com.udyneos.resteam

import android.app.Application
import com.udyneos.resteam.domain.repository.ScriptRepository
import com.udyneos.resteam.domain.usecase.GameSessionManager
import com.udyneos.resteam.session.GameSessionService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class LauncherApplication : Application() {

    @Inject lateinit var scripts: ScriptRepository
    @Inject lateinit var sessions: GameSessionManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Lanjutkan restore yang tertunda (ReSteam sempat ditutup/di-kill saat game masih berjalan)
        appScope.launch {
            if (scripts.getPendingRestores().isEmpty()) return@launch
            if (!GameSessionService.resume(this@LauncherApplication)) {
                // Service tidak boleh dimulai dari background: kerjakan langsung selama proses hidup
                sessions.pending().map { it.gamePackage }.distinct().forEach {
                    sessions.watchAndRestore(it, waitForStart = false)
                }
            }
        }
    }
}
