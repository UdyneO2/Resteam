package com.example.launcher

import com.example.launcher.data.InMemoryActivityLog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InMemoryActivityLogTest {

    @Test
    fun `record menambah entri dengan urutan masuk`() {
        val log = InMemoryActivityLog()

        log.record("a", "detail a")
        log.record("b", "detail b", isError = true)

        assertEquals(listOf("a", "b"), log.entries.value.map { it.title })
        assertTrue(log.entries.value.last().isError)
    }

    @Test
    fun `jumlah entri dibatasi 200 dan yang tertua dibuang`() {
        val log = InMemoryActivityLog()

        repeat(250) { log.record("entri $it", "") }

        assertEquals(200, log.entries.value.size)
        assertEquals("entri 50", log.entries.value.first().title)
    }

    @Test
    fun `clear mengosongkan log`() {
        val log = InMemoryActivityLog()
        log.record("a", "")

        log.clear()

        assertTrue(log.entries.value.isEmpty())
    }
}
