package com.retrorts

import com.retrorts.ui.GamePathValidator
import com.retrorts.ui.PerfStats
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BridgeAndValidationTest {

    @Test
    fun `path validator accepts sdcard and content uris`() {
        assertTrue(GamePathValidator.isValid("/sdcard/RetroRTS/Games/Dune2000"))
        assertTrue(GamePathValidator.isValid("content://com.android.externalstorage.documents/tree/primary%3ARetroRTS"))
    }

    @Test
    fun `path validator rejects invalid inputs`() {
        assertFalse(GamePathValidator.isValid(""))
        assertFalse(GamePathValidator.isValid("/data/local/tmp/game"))
        assertFalse(GamePathValidator.isValid("http://example.com/game"))
    }

    @Test
    fun `perf stats model holds fps and cpu usage`() {
        val stats = PerfStats(59.8f, 72.3f)
        assertTrue(stats.fps > 0f)
        assertTrue(stats.cpuUsagePercent in 0f..100f)
    }
}
