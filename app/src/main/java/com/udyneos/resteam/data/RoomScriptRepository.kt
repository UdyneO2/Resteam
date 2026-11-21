package com.udyneos.resteam.data

import com.udyneos.resteam.data.local.ScriptDao
import com.udyneos.resteam.data.local.toDomain
import com.udyneos.resteam.data.local.toEntity
import com.udyneos.resteam.domain.model.PendingRestore
import com.udyneos.resteam.domain.model.ShizukuScript
import com.udyneos.resteam.domain.repository.ScriptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomScriptRepository @Inject constructor(
    private val dao: ScriptDao,
) : ScriptRepository {

    override fun observeScripts(): Flow<List<ShizukuScript>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getEnabledScripts(): List<ShizukuScript> = dao.getEnabled().map { it.toDomain() }

    override suspend fun getScript(fileName: String): ShizukuScript? = dao.get(fileName)?.toDomain()

    override suspend fun setEnabled(fileName: String, enabled: Boolean) = dao.setEnabled(fileName, enabled)

    override suspend fun replaceAll(fetched: List<ShizukuScript>) = dao.sync(fetched.map { it.toEntity() })

    override suspend fun getPendingRestores(): List<PendingRestore> =
        dao.getPendingRestores().map { it.toDomain() }

    override suspend fun markApplied(restore: PendingRestore) = dao.addPendingRestore(restore.toEntity())

    override suspend fun markRestored(fileName: String) = dao.clearPendingRestore(fileName)
}
