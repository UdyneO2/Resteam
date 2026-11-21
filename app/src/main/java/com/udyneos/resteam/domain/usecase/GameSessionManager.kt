package com.udyneos.resteam.domain.usecase

import com.udyneos.resteam.domain.model.PendingRestore
import com.udyneos.resteam.domain.model.ShizukuScript
import com.udyneos.resteam.domain.model.ShizukuState
import com.udyneos.resteam.domain.repository.ActivityLog
import com.udyneos.resteam.domain.repository.ScriptRepository
import com.udyneos.resteam.domain.repository.SettingsRepository
import com.udyneos.resteam.domain.repository.ShellExecutor
import com.udyneos.resteam.shizuku.ShizukuManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Siklus script per sesi game: `apply` saat Launch, pantau sampai game ditutup, lalu `restore`.
 * Script yang sudah di-apply dicatat di database lebih dulu, sehingga restore tetap bisa dilanjutkan
 * walau ReSteam ditutup atau di-kill di tengah sesi.
 */
@Singleton
class GameSessionManager @Inject constructor(
    private val shell: ShellExecutor,
    private val scripts: ScriptRepository,
    private val settings: SettingsRepository,
    private val shizuku: ShizukuManager,
    private val log: ActivityLog,
) {
    private val mutex = Mutex()

    /** True jika ada yang perlu dikerjakan: toggle global menyala dan ada script aktif atau restore tertunda. */
    suspend fun hasWork(): Boolean =
        scripts.getPendingRestores().isNotEmpty() ||
            (settings.settings.first().shizukuTweaksEnabled && scripts.getEnabledScripts().isNotEmpty())

    suspend fun pending(): List<PendingRestore> = scripts.getPendingRestores()

    /** Menjalankan `apply` semua script aktif. Mengembalikan jumlah script yang dicatat untuk di-restore. */
    suspend fun begin(gamePackage: String): Int = mutex.withLock {
        if (!settings.settings.first().shizukuTweaksEnabled) return 0
        val enabled = scripts.getEnabledScripts()
        if (enabled.isEmpty()) return 0
        if (!awaitShizukuReady()) {
            log.record("Shizuku Tweaks dilewati", "Shizuku belum siap, game dibuka tanpa script", isError = true)
            return 0
        }
        var recorded = 0
        for (script in enabled) {
            // Dicatat SEBELUM dijalankan: jika proses mati di tengah, restore tetap tahu apa yang harus dikembalikan
            scripts.markApplied(PendingRestore(script.fileName, gamePackage, System.currentTimeMillis()))
            recorded++
            runScript(script, MODE_APPLY)
        }
        recorded
    }

    /**
     * Menunggu [gamePackage] ditutup lalu menjalankan `restore`.
     * [waitForStart]: beri waktu game muncul setelah Launch; false saat melanjutkan sesi lama.
     */
    suspend fun watchAndRestore(gamePackage: String, waitForStart: Boolean) {
        var seenRunning = !waitForStart
        var absentPolls = 0
        var failures = 0
        var waited = 0L
        while (true) {
            when (isRunning(gamePackage)) {
                true -> {
                    seenRunning = true
                    absentPolls = 0
                    failures = 0
                }
                false -> {
                    failures = 0
                    if (seenRunning) {
                        absentPolls++
                        if (absentPolls >= ABSENT_POLLS_TO_CLOSE) break
                    } else {
                        waited += POLL_MS
                        if (waited >= START_GRACE_MS) break // game tidak pernah muncul
                    }
                }
                null -> {
                    // Shizuku tidak bisa dihubungi: jangan restore, coba lagi nanti
                    if (++failures >= MAX_FAILURES) {
                        log.record("Restore tertunda", "Shizuku tidak merespons; dilanjutkan saat ReSteam dibuka lagi", isError = true)
                        return
                    }
                }
            }
            delay(POLL_MS)
        }
        restorePending(gamePackage)
    }

    /** Menjalankan `restore` untuk semua catatan tertunda milik [gamePackage] (atau semuanya jika null). */
    suspend fun restorePending(gamePackage: String? = null): Unit = mutex.withLock {
        val items = scripts.getPendingRestores()
            .filter { gamePackage == null || it.gamePackage == gamePackage }
            .sortedByDescending { it.appliedAt } // urutan terbalik dari apply
        if (items.isEmpty()) return
        if (!awaitShizukuReady()) {
            log.record("Restore tertunda", "Shizuku belum siap; akan dicoba lagi saat ReSteam dibuka", isError = true)
            return
        }
        for (item in items) {
            val script = scripts.getScript(item.fileName)
            if (script == null) {
                log.record("Restore dilewati", "${item.fileName} sudah tidak ada di daftar script", isError = true)
            } else {
                runScript(script, MODE_RESTORE)
            }
            scripts.markRestored(item.fileName)
        }
    }

    /** True/false jika status game diketahui; null jika Shizuku gagal menjawab. */
    private suspend fun isRunning(packageName: String): Boolean? {
        if (!isValidPackage(packageName)) return null
        val result = shell.run(listOf("pidof", packageName)).getOrNull() ?: return null
        return result.isSuccess && result.output.isNotBlank()
    }

    private suspend fun runScript(script: ShizukuScript, mode: String) {
        val title = "${script.name} ($mode)"
        if (script.content.length > MAX_SCRIPT_CHARS) {
            log.record(title, "Script terlalu besar untuk dijalankan", isError = true)
            return
        }
        // Isi script dikirim sebagai argumen `sh -c`; $1 = mode. Bukan lewat string shell buatan sendiri.
        val command = listOf("sh", "-c", script.content, "resteam", mode)
        shell.runWithTimeout(command, SCRIPT_TIMEOUT_MS).fold(
            onSuccess = { result ->
                log.record(
                    title = title,
                    detail = if (result.isSuccess) {
                        result.output.ifBlank { "Selesai" }
                    } else {
                        "Exit ${result.exitCode}: ${result.output.ifBlank { "(tanpa output)" }}"
                    },
                    isError = !result.isSuccess,
                )
            },
            onFailure = { log.record(title, it.message ?: "Gagal menjalankan script", isError = true) },
        )
    }

    private suspend fun awaitShizukuReady(): Boolean =
        withTimeoutOrNull(SHIZUKU_WAIT_MS) { shizuku.state.first { it is ShizukuState.Ready } } != null

    private fun isValidPackage(value: String) = PACKAGE.matches(value)

    private companion object {
        const val MODE_APPLY = "apply"
        const val MODE_RESTORE = "restore"
        const val POLL_MS = 5_000L
        const val START_GRACE_MS = 90_000L
        const val ABSENT_POLLS_TO_CLOSE = 2
        const val MAX_FAILURES = 120
        const val SCRIPT_TIMEOUT_MS = 60_000L
        const val SHIZUKU_WAIT_MS = 10_000L
        const val MAX_SCRIPT_CHARS = 100_000
        val PACKAGE = Regex("""[A-Za-z0-9_.]{1,150}""")
    }
}
