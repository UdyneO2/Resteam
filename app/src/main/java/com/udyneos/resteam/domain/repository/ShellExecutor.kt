package com.udyneos.resteam.domain.repository

import com.udyneos.resteam.domain.model.CommandResult

interface ShellExecutor {
    /** Command berupa array argumen (bukan string shell) sehingga tidak ada risiko command injection. */
    suspend fun run(command: List<String>): Result<CommandResult>

    /** Seperti [run], tetapi proses dihentikan setelah [timeoutMs]; hasilnya exit code 124. */
    suspend fun runWithTimeout(command: List<String>, timeoutMs: Long): Result<CommandResult> = run(command)
}
