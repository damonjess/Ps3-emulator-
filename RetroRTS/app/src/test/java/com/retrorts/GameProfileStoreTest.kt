package com.retrorts

import com.retrorts.ui.GameProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameProfileStoreTest {

    @Test
    fun `profile json roundtrip preserves fields`() {
        val profile = GameProfile.presetDune2000Win98()
        val json = profile.toJson()
        val loaded = GameProfile.fromJson(json)

        assertEquals(profile.gameId, loaded.gameId)
        assertEquals(profile.title, loaded.title)
        assertEquals(profile.os, loaded.os)
        assertEquals(profile.cycles, loaded.cycles)
        assertEquals(profile.frameCap, loaded.frameCap)
    }

    @Test
    fun `dosbox config contains expected tuned keys`() {
        val profile = GameProfile.presetRedAlert95()
        val config = profile.toDosboxConfig()

        assertTrue(config.contains("machine=svga_s3"))
        assertTrue(config.contains("core=dynamic"))
        assertTrue(config.contains("cycles=30000"))
    }

    @Test
    fun `amiga profile maps to native amiga platform`() {
        val profile = GameProfile.presetAmigaA500()

        assertEquals("amiga", profile.platform)
        assertEquals("amiga_a500", profile.machine)
        assertEquals(50, profile.frameCap)
    }

    @Test
    fun `dsi profile maps to native dsi platform`() {
        val profile = GameProfile.presetNintendoDsi()

        assertEquals("dsi", profile.platform)
        assertEquals("nintendo_dsi", profile.machine)
        assertEquals(60, profile.frameCap)
    }
}
