package com.udyneos.resteam

import com.udyneos.resteam.domain.model.ActivityLevel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ModelsTest {

    @Test
    fun `fromHours memetakan ambang batas dengan benar`() {
        assertEquals(ActivityLevel.None, ActivityLevel.fromHours(0f))
        assertEquals(ActivityLevel.None, ActivityLevel.fromHours(2f))
        assertEquals(ActivityLevel.Low, ActivityLevel.fromHours(2.1f))
        assertEquals(ActivityLevel.Medium, ActivityLevel.fromHours(5f))
        assertEquals(ActivityLevel.High, ActivityLevel.fromHours(9f))
    }
}
