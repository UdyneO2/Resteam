package com.udyneos.resteam.shizuku

import android.content.pm.PackageManager
import com.udyneos.resteam.domain.model.ShizukuMode
import com.udyneos.resteam.domain.model.ShizukuState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShizukuManager @Inject constructor() {

    /** Status Shizuku secara reaktif; listener otomatis dilepas saat flow berhenti dikoleksi. */
    val state: Flow<ShizukuState> = callbackFlow {
        val push = { trySend(readState()) }
        val onBinderReceived = Shizuku.OnBinderReceivedListener { push() }
        val onBinderDead = Shizuku.OnBinderDeadListener { push() }
        val onPermissionResult = Shizuku.OnRequestPermissionResultListener { _, _ -> push() }

        Shizuku.addBinderReceivedListenerSticky(onBinderReceived)
        Shizuku.addBinderDeadListener(onBinderDead)
        Shizuku.addRequestPermissionResultListener(onPermissionResult)
        push()

        awaitClose {
            Shizuku.removeBinderReceivedListener(onBinderReceived)
            Shizuku.removeBinderDeadListener(onBinderDead)
            Shizuku.removeRequestPermissionResultListener(onPermissionResult)
        }
    }.distinctUntilChanged()

    fun requestPermission() {
        runCatching { Shizuku.requestPermission(REQUEST_CODE) }
    }

    private fun readState(): ShizukuState = runCatching {
        when {
            !Shizuku.pingBinder() -> ShizukuState.Unavailable
            Shizuku.isPreV11() -> ShizukuState.Error("Versi Shizuku terlalu lama")
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED ->
                ShizukuState.Ready(if (Shizuku.getUid() == 0) ShizukuMode.Root else ShizukuMode.Adb)
            Shizuku.shouldShowRequestPermissionRationale() -> ShizukuState.PermissionDenied
            else -> ShizukuState.PermissionRequired
        }
    }.getOrElse { ShizukuState.Error(it.message ?: "Kesalahan Shizuku") }

    private companion object {
        const val REQUEST_CODE = 1001
    }
}
