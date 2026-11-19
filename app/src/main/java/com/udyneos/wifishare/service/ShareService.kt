package com.udyneos.wifishare.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.udyneos.wifishare.MainActivity
import com.udyneos.wifishare.R
import com.udyneos.wifishare.data.AppSettings
import com.udyneos.wifishare.data.ShareState
import com.udyneos.wifishare.proxy.ProxyServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Menjaga grup WiFi Direct dan proxy tetap hidup walau aplikasi ditutup.
 * Berhenti sendiri kalau grup bubar atau gagal dibuat.
 */
class ShareService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var watching: Job? = null
    private var proxy: ProxyServer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                ShareHub.controller(this).stop()
                finish()
            }
            else -> begin()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        // dipanggil juga kalau sistem mematikan service, pastikan grup dan proxy ikut bersih
        proxy?.stop()
        proxy = null
        scope.cancel()
        super.onDestroy()
    }

    private fun begin() {
        createChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification("Menyiapkan WiFi Direct…"),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
        )

        val controller = ShareHub.controller(this)
        controller.start()

        watching?.cancel()
        watching = scope.launch {
            controller.state.collect { render(it) }
        }
    }

    private fun render(state: ShareState) {
        when {
            state.active -> {
                startProxyIfNeeded()
                val text = when (state.clients.size) {
                    0 -> "Menunggu perangkat tersambung"
                    else -> "${state.clients.size} perangkat tersambung · ${state.proxyAddress}"
                }
                notificationManager().notify(NOTIFICATION_ID, buildNotification(text))
            }
            state.busy -> Unit // masih menyiapkan grup
            else -> finish() // gagal atau grup bubar
        }
    }

    private fun startProxyIfNeeded() {
        if (proxy != null) return

        val port = AppSettings(this).proxyPort
        val server = ProxyServer(port)
        if (server.start()) {
            proxy = server
        } else {
            ShareHub.controller(this).abort("Port $port sedang dipakai, ganti port di Pengaturan")
        }
    }

    private fun finish() {
        watching?.cancel()
        watching = null
        proxy?.stop()
        proxy = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(text: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, ShareService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_share)
            .setContentTitle("Sedang berbagi internet")
            .setContentText(text)
            .setContentIntent(open)
            .addAction(0, "Berhenti", stop)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Berbagi internet",
            NotificationManager.IMPORTANCE_LOW,
        )
        notificationManager().createNotificationChannel(channel)
    }

    private fun notificationManager() =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val ACTION_START = "com.udyneos.wifishare.START"
        const val ACTION_STOP = "com.udyneos.wifishare.STOP"

        private const val CHANNEL_ID = "sharing"
        private const val NOTIFICATION_ID = 1

        fun startIntent(context: Context): Intent =
            Intent(context, ShareService::class.java).setAction(ACTION_START)

        fun stopIntent(context: Context): Intent =
            Intent(context, ShareService::class.java).setAction(ACTION_STOP)
    }
}
