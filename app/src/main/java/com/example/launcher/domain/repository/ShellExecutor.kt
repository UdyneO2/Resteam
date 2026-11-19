package com.example.launcher.domain.repository

import com.example.launcher.domain.model.CommandResult

interface ShellExecutor {
    /** Command berupa array argumen (bukan string shell) sehingga tidak ada risiko command injection. */
    suspend fun run(command: List<String>): Result<CommandResult>
}
