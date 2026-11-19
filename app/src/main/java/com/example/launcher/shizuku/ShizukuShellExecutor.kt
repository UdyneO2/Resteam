package com.example.launcher.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import com.example.launcher.BuildConfig
import com.example.launcher.domain.model.CommandResult
import com.example.launcher.domain.repository.ShellExecutor
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

    override suspend fun run(command: List<String>): Result<CommandResult> = mutex.withLock {
        runCatching {
            val shell = withTimeoutOrNull(CONNECT_TIMEOUT_MS) { connect() }
                ?: error("Layanan Shizuku tidak merespons")
            val raw = withContext(Dispatchers.IO) { shell.exec(command.toTypedArray()) }
            CommandResult(
                command = command.joinToString(" "),
                exitCode = raw.substringBefore('\n').toIntOrNull() ?: -1,
                output = raw.substringAfter('\n', missingDelimiterValue = "").trim(),
            )
        }.onFailure { if (it is CancellationException) throw it }
    }

    private suspend fun connect(): IShellService {
        service?.takeIf { it.asBinder().pingBinder() }?.let { return it }

        return suspendCancellableCoroutine { continuation ->
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                    val connected = IShellService.Stub.asInterface(binder)
                    service = connected
                    if (continuation.isActive) continuation.resume(connected)
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    service = null
                }
            }
            Shizuku.bindUserService(serviceArgs, connection)
            continuation.invokeOnCancellation {
                runCatching { Shizuku.unbindUserService(serviceArgs, connection, true) }
            }
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 10_000L
    }
}
