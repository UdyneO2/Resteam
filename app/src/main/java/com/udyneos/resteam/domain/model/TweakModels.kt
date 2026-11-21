package com.udyneos.resteam.domain.model

/**
 * [arg] disimpan di database. [code] dikirim ke `cmd game set --mode`: angka konstanta GameManager
 * (STANDARD=1, PERFORMANCE=2, BATTERY=3), karena sebagian versi Android menolak nama
 * (NumberFormatException: For input string: "performance").
 */
enum class GameMode(val arg: String, val label: String, val code: Int) {
    Standard("standard", "Standard", 1),
    Performance("performance", "Performance", 2),
    Battery("battery", "Battery", 3);

    companion object {
        fun fromArg(arg: String): GameMode = entries.firstOrNull { it.arg == arg } ?: Standard
    }
}

enum class CompileFilter(val arg: String) {
    SpeedProfile("speed-profile"),
    Speed("speed");

    companion object {
        fun fromArg(arg: String?): CompileFilter? = entries.firstOrNull { it.arg == arg }
    }
}

data class TweakPreset(
    val id: Long,
    val gameId: String,
    val name: String,
    val gameMode: GameMode,
    val compileFilter: CompileFilter? = null,
) {
    fun describe(): String = buildString {
        append("Game mode: ${gameMode.label}")
        compileFilter?.let { append(" • Compile: ${it.arg}") }
    }
}

data class CommandResult(
    val command: String,
    val exitCode: Int,
    val output: String,
) {
    val isSuccess: Boolean get() = exitCode == 0
}

class CommandFailedException(val result: CommandResult) :
    Exception("Perintah gagal (exit ${result.exitCode}): ${result.output.ifBlank { result.command }}")

/** Profil per game: preset default dan apakah otomatis diterapkan saat game diluncurkan dari launcher. */
data class GameProfile(
    val gameId: String,
    val presetId: Long?,
    val autoApply: Boolean,
)
