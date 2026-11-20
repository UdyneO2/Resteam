package com.example.launcher.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Penanda bahwa preset default untuk game ini sudah pernah dibuat (agar tidak muncul lagi setelah dihapus user). */
@Entity(tableName = "preset_seeds")
data class PresetSeedEntity(@PrimaryKey val gameId: String)
