package com.udyneos.resteam.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.udyneos.resteam.domain.model.PendingRestore
import com.udyneos.resteam.domain.model.ShizukuScript

@Entity(tableName = "scripts")
data class ScriptEntity(
    @PrimaryKey val fileName: String,
    val name: String,
    val description: String,
    val version: String,
    val content: String,
    val sha: String,
    val enabled: Boolean,
)

@Entity(tableName = "pending_restores")
data class PendingRestoreEntity(
    @PrimaryKey val fileName: String,
    val gamePackage: String,
    val appliedAt: Long,
)

fun ScriptEntity.toDomain() = ShizukuScript(fileName, name, description, version, content, sha, enabled)
fun ShizukuScript.toEntity() = ScriptEntity(fileName, name, description, version, content, sha, enabled)
fun PendingRestoreEntity.toDomain() = PendingRestore(fileName, gamePackage, appliedAt)
fun PendingRestore.toEntity() = PendingRestoreEntity(fileName, gamePackage, appliedAt)
