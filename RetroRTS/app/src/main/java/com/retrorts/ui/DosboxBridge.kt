package com.retrorts.ui
import android.util.Log
class DosboxBridge {
companion object {
private const val TAG = "DosboxBridge"
    val isAvailable: Boolean = runCatching {
        System.loadLibrary("retrorts_jni")
        true
    }.getOrElse {
        Log.e(TAG, "Failed to load retrorts_jni: ${it.message}")
        false
    }

    fun startDosbox(gameDir: String, configPath: String): Boolean {
        if (!isAvailable) return false
        return runCatching {
            startDosboxNative(gameDir, configPath)
        }.getOrElse {
            Log.e(TAG, "startDosbox error: ${it.message}")
            false
        }
    }

    fun stopDosbox() {
        if (!isAvailable) return
        runCatching { stopDosboxNative() }
    }

    fun setCpuCycles(cycles: Int) {
        if (isAvailable) runCatching { setCpuCyclesNative(cycles) }
    }

    fun setFrameCap(fps: Int) {
        if (isAvailable) runCatching { setFrameCapNative(fps) }
    }

    fun setVolume(volume: Float) {
        if (isAvailable) runCatching { setVolumeNative(volume) }
    }

    fun notifyThermalLevel(level: Int) {
        if (isAvailable) runCatching { notifyThermalLevelNative(level) }
    }

    fun getPerfStats(): FloatArray {
        if (!isAvailable) return floatArrayOf(0f, 0f)
        return runCatching { getPerfStatsNative() }.getOrDefault(floatArrayOf(0f, 0f))
    }

    fun saveState(gameId: String, slot: Int, path: String): Boolean {
        if (!isAvailable) return false
        return runCatching { saveStateNative(gameId, slot, path) }.getOrDefault(false)
    }

    fun loadState(gameId: String, slot: Int, path: String): Boolean {
        if (!isAvailable) return false
        return runCatching { loadStateNative(gameId, slot, path) }.getOrDefault(false)
    }

    // ── native declarations ─────────────────────────────────────────
    @JvmStatic private external fun startDosboxNative(gameDir: String, configPath: String): Boolean
    @JvmStatic private external fun stopDosboxNative()
    @JvmStatic private external fun setCpuCyclesNative(cycles: Int)
    @JvmStatic private external fun setFrameCapNative(fps: Int)
    @JvmStatic private external fun setVolumeNative(volume: Float)
    @JvmStatic private external fun notifyThermalLevelNative(level: Int)
    @JvmStatic private external fun getPerfStatsNative(): FloatArray
    @JvmStatic private external fun saveStateNative(gameId: String, slot: Int, path: String): Boolean
    @JvmStatic private external fun loadStateNative(gameId: String, slot: Int, path: String): Boolean
}
}