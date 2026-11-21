package com.udyneos.resteam.domain.repository

/** Satu file .sh di folder scripts/ repo GitHub (belum diunduh isinya). */
data class RemoteScriptFile(
    val fileName: String,
    val sha: String,
    val downloadUrl: String,
    val size: Long,
)

interface RemoteScriptSource {
    /** Daftar file .sh di scripts/ pada branch tertentu. */
    suspend fun listScripts(repo: String, branch: String): Result<List<RemoteScriptFile>>

    suspend fun download(file: RemoteScriptFile): Result<String>

    /** Nama branch repo, untuk dipilih di aplikasi. */
    suspend fun listBranches(repo: String): Result<List<String>>
}
