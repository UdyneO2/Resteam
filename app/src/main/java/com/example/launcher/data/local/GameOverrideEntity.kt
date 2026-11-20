package com.example.launcher.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Pilihan manual user: `included = true` paksa jadi game, `false` paksa keluar dari daftar. */
@Entity(tableName = "game_overrides")
data class GameOverrideEntity(
    @PrimaryKey val packageName: String,
    val included: Boolean,
)
