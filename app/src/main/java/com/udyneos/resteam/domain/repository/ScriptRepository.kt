package com.udyneos.resteam.domain.repository

import com.udyneos.resteam.domain.model.PendingRestore
import com.udyneos.resteam.domain.model.ShizukuScript
import kotlinx.coroutines.flow.Flow

interface ScriptRepository {
    fun observeScripts(): Flow<List<ShizukuScript>>
    suspend fun getEnabledScripts(): List<ShizukuScript>
    suspend fun getScript(fileName: String): ShizukuScript?
    suspend fun setEnabled(fileName: String, enabled: Boolean)

    /** Mengganti daftar lokal dengan hasil fetch; toggle yang sudah ada dipertahankan. */
    suspend fun replaceAll(fetched: List<ShizukuScript>)

    suspend fun getPendingRestores(): List<PendingRestore>
    suspend fun markApplied(restore: PendingRestore)
    suspend fun markRestored(fileName: String)
}
