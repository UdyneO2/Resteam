package com.example.launcher.shizuku

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
}
