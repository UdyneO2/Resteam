package com.udyneos.resteam.shizuku

import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/** Berjalan di proses Shizuku (shell/root), BUKAN di proses aplikasi. */
class ShellService : IShellService.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    override fun exec(command: Array<String>): String {
        val process = ProcessBuilder(command.toList())
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()
        return "$exitCode\n$output"
    }

    override fun execTimeout(command: Array<String>, timeoutMs: Long): String {
        val process = ProcessBuilder(command.toList())
            .redirectErrorStream(true)
            .start()
        val output = StringBuffer()
        val reader = Thread { runCatching { output.append(process.inputStream.bufferedReader().readText()) } }
            .apply { isDaemon = true; start() }
        if (!process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
            process.destroyForcibly()
            reader.join(1_000)
            return "124\n$output\n[dihentikan: melewati batas waktu ${timeoutMs / 1000} detik]"
        }
        reader.join(2_000)
        return "${process.exitValue()}\n$output"
    }
}
