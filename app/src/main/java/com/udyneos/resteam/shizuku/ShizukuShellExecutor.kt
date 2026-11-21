package com.udyneos.resteam.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import com.udyneos.resteam.BuildConfig
import com.udyneos.resteam.domain.model.CommandResult
import com.udyneos.resteam.domain.repository.ShellExecutor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class ShizukuShellExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
) : ShellExecutor {

    private val mutex = Mutex()
    private var service: IShellService? = null

    private val serviceArgs = Shizuku.UserServiceArgs(
        ComponentName(context.packageName, ShellService::class.java.name),
    )
        .daemon(false)
        .processNameSuffix("shell")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    override suspend fun run(command: List<String>): Result<CommandResult> = execute(command, timeoutMs = null)

    override suspend fun runWithTimeout(command: List<String>, timeoutMs: Long): Result<CommandResult> =
        execute(command, timeoutMs)

    private suspend fun execute(command: List<String>, timeoutMs: Long?): Result<CommandResult> = mutex.withLock {
        runCatching {
            val shell = withTimeoutOrNull(CONNECT_TIMEOUT_MS) { connect() }
                ?: error("Layanan Shizuku tidak merespons")
            val raw = withContext(Dispatchers.IO) {
                if (timeoutMs == null) shell.exec(command.toTypedArray()) else shell.execTimeout(command.toTypedArray(), timeoutMs)
            }
            CommandResult(
                command = command.joinToString(" "),
                exitCode = raw.substringBefore('\n').toIntOrNull() ?: -1,
                output = raw.substringAfter('\n', missingDelimiterValue = "").trim(),
            )
        }.onFailure {
            release()
            if (it is CancellationException) throw it
        }
    }

    private var connection: ServiceConnection? = null

    private suspend fun connect(): IShellService {
        service?.takeIf { it.asBinder().pingBinder() }?.let { return it }
        // Koneksi lama basi: lepas dulu agar bind baru tidak menumpuk
        release()

        return suspendCancellableCoroutine { continuation ->
            val conn = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                    val connected = IShellService.Stub.asInterface(binder)
                    service = connected
                    if (continuation.isActive) continuation.resume(connected)
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    service = null
                }

                override fun onNullBinding(name: ComponentName) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(IllegalStateException("Layanan Shizuku mengembalikan binder kosong"))
                    }
                }

                override fun onBindingDied(name: ComponentName) {
                    service = null
                    if (continuation.isActive) {
                        continuation.resumeWithException(IllegalStateException("Layanan Shizuku mati saat dihubungkan"))
                    }
                }
            }
            connection = conn
            try {
                Shizuku.bindUserService(serviceArgs, conn)
            } catch (t: Throwable) {
                if (continuation.isActive) continuation.resumeWithException(t)
            }
            continuation.invokeOnCancellation { release() }
        }
    }

    private fun release() {
        connection?.let { runCatching { Shizuku.unbindUserService(serviceArgs, it, true) } }
        connection = null
        service = null
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 10_000L
    }
}
