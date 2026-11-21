package com.udyneos.resteam.session

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.udyneos.resteam.domain.usecase.GameSessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Menjalankan siklus script (apply, pantau game, restore). Baru dijadikan foreground service
 * jika memang ada pekerjaan, supaya tidak ada notifikasi tanpa guna dan restore tidak ikut
 * mati saat ReSteam di-swipe dari recent apps.
 */
@AndroidEntryPoint
class GameSessionService : Service() {

    @Inject lateinit var manager: GameSessionManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (job?.isActive == true) return START_NOT_STICKY
        val action = intent?.action
        val gamePackage = intent?.getStringExtra(EXTRA_PACKAGE)
        job = scope.launch {
            try {
                if (!manager.hasWork()) return@launch
                promoteToForeground()
                when {
                    action == ACTION_START && gamePackage != null -> {
                        manager.begin(gamePackage)
                        manager.watchAndRestore(gamePackage, waitForStart = true)
                    }
                    action == ACTION_RESUME -> resumePending()
                }
            } finally {
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun resumePending() {
        manager.pending().map { it.gamePackage }.distinct().forEach {
            manager.watchAndRestore(it, waitForStart = false)
        }
    }

    private fun promoteToForeground() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Sesi game", NotificationManager.IMPORTANCE_LOW),
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_preferences)
            .setContentTitle("ReSteam")
            .setContentText("Shizuku Tweaks aktif; nilai dikembalikan setelah game ditutup")
            .setOngoing(true)
            .build()
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        // Gagal dipromosikan (mis. dimulai dari background): tetap lanjut sebagai service biasa
        runCatching { ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.udyneos.resteam.session.START"
        const val ACTION_RESUME = "com.udyneos.resteam.session.RESUME"
        const val EXTRA_PACKAGE = "package"
        private const val CHANNEL_ID = "game_session"
        private const val NOTIFICATION_ID = 4101

        fun start(context: Context, gamePackage: String): Boolean = send(
            context,
            Intent(context, GameSessionService::class.java).setAction(ACTION_START).putExtra(EXTRA_PACKAGE, gamePackage),
        )

        fun resume(context: Context): Boolean =
            send(context, Intent(context, GameSessionService::class.java).setAction(ACTION_RESUME))

        // startService biasa (bukan startForegroundService): service baru menjadi foreground jika ada pekerjaan
        private fun send(context: Context, intent: Intent): Boolean =
            runCatching { context.startService(intent) }.isSuccess
    }
}
