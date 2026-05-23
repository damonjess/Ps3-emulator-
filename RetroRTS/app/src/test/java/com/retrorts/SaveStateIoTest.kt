package com.retrorts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SaveStateIoTest {

    @Test
    fun `save state write and read placeholder bytes`() {
        val temp = File.createTempFile("retrorts_state", ".sav")
        val payload = "GAME=dune_2000\nSLOT=1\nSTATE=PLACEHOLDER\n"

        temp.writeText(payload)
        assertTrue(temp.exists())

        val readBack = temp.readText()
        assertEquals(payload, readBack)

        temp.delete()
    }
}
