package com.udyneos.resteam.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object LauncherShapes {
    val Card = RoundedCornerShape(24.dp)
    val Cell = RoundedCornerShape(8.dp)
    val Pill = CircleShape
}

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = LauncherShapes.Card,
)
