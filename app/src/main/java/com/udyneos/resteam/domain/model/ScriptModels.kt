package com.udyneos.resteam.domain.model

/**
 * Script tweak global dari folder scripts/ di repo GitHub.
 *
 * Konvensi eksekusi: `sh <file>.sh apply` untuk menerapkan dan `sh <file>.sh restore`
 * untuk mengembalikan nilai asli setelah game ditutup.
 */
data class ShizukuScript(
    /** Nama file, mis. "gpu_boost.sh". Dipakai sebagai id unik. */
    val fileName: String,
    val name: String,
    val description: String,
    val version: String,
    val content: String,
    /** SHA blob dari GitHub; dipakai untuk mendeteksi perubahan tanpa mengunduh ulang. */
    val sha: String,
    val enabled: Boolean,
)

/** Metadata dari komentar header script (`# name:`, `# desc:`, `# version:`). */
data class ScriptMetadata(
    val name: String,
    val description: String,
    val version: String,
)

/** Script yang sudah di-apply dan menunggu restore (bertahan walau ReSteam ditutup). */
data class PendingRestore(
    val fileName: String,
    val gamePackage: String,
    val appliedAt: Long,
)

/** Mengambil metadata dari 30 baris pertama; baris di luar format `# kunci: nilai` diabaikan. */
fun parseScriptMetadata(fileName: String, content: String): ScriptMetadata {
    val values = HashMap<String, String>()
    content.lineSequence().take(30).forEach { line ->
        val match = METADATA_LINE.matchEntire(line.trim()) ?: return@forEach
        values.putIfAbsent(match.groupValues[1].lowercase(), match.groupValues[2].trim())
    }
    return ScriptMetadata(
        name = values["name"]?.takeIf { it.isNotBlank() } ?: fileName.removeSuffix(".sh"),
        description = values["desc"] ?: values["description"] ?: "",
        version = values["version"] ?: "1",
    )
}

private val METADATA_LINE = Regex("""#\s*(name|desc|description|version)\s*:\s*(.*)""", RegexOption.IGNORE_CASE)
