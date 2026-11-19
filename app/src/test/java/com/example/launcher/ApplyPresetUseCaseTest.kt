package com.example.launcher

import com.example.launcher.domain.model.CommandFailedException
import com.example.launcher.domain.model.CommandResult
import com.example.launcher.domain.model.CompileFilter
import com.example.launcher.domain.model.GameMode
import com.example.launcher.domain.model.TweakPreset
import com.example.launcher.domain.repository.ShellExecutor
import com.example.launcher.domain.usecase.ApplyPresetUseCase
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ApplyPresetUseCaseTest {

    private class FakeShell(private val exitCodes: List<Int> = emptyList()) : ShellExecutor {
        val executed = mutableListOf<List<String>>()

        override suspend fun run(command: List<String>): Result<CommandResult> {
            executed += command
            val code = exitCodes.getOrElse(executed.size - 1) { 0 }
            return Result.success(CommandResult(command.joinToString(" "), code, ""))
        }
    }

    private val preset = TweakPreset(
        id = 1,
        gameId = "g",
        name = "Performance",
        gameMode = GameMode.Performance,
        compileFilter = CompileFilter.SpeedProfile,
    )

    @Test
    fun `menjalankan game mode lalu compile sesuai urutan`() = runTest {
        val shell = FakeShell()

        val result = ApplyPresetUseCase(shell)(preset, "com.test.game")

        assertTrue(result.isSuccess)
        assertEquals(
            listOf(
                listOf("cmd", "game", "set", "--mode", "performance", "com.test.game"),
                listOf("cmd", "package", "compile", "-m", "speed-profile", "-f", "com.test.game"),
            ),
            shell.executed,
        )
    }

    @Test
    fun `package tidak valid ditolak tanpa menjalankan perintah`() = runTest {
        val shell = FakeShell()

        val result = ApplyPresetUseCase(shell)(preset, "com.test; rm -rf /")

        assertTrue(result.isFailure)
        assertTrue(shell.executed.isEmpty())
    }

    @Test
    fun `berhenti di perintah pertama yang gagal`() = runTest {
        val shell = FakeShell(exitCodes = listOf(1))

        val result = ApplyPresetUseCase(shell)(preset, "com.test.game")

        assertTrue(result.exceptionOrNull() is CommandFailedException)
        assertEquals(1, shell.executed.size)
    }
}
