package com.retrorts.ui

data class PerfStats(
    val fps: Float,
    val cpuUsagePercent: Float,
)

object DosboxBridge {
    private val nativeLoaded = runCatching {
        System.loadLibrary("retrorts_dosbox_jni")
    }.isSuccess

    fun startDosbox(gameDirectory: String, configPath: String): Boolean =
        nativeLoaded && runCatching { startDosboxNative(gameDirectory, configPath) }.getOrDefault(false)

    fun stopDosbox() {
        if (nativeLoaded) runCatching { stopDosboxNative() }
    }

    fun setVolume(volume: Float) {
        if (nativeLoaded) runCatching { setVolumeNative(volume) }
    }

    fun submitAudioPcm16(buffer: ShortArray, frames: Int, channels: Int) {
        if (nativeLoaded) runCatching { submitAudioPcm16Native(buffer, frames, channels) }
    }

    fun saveState(gameId: String, slot: Int, path: String): Boolean =
        nativeLoaded && runCatching { saveStateNative(gameId, slot, path) }.getOrDefault(false)

    fun loadState(gameId: String, slot: Int, path: String): Boolean =
        nativeLoaded && runCatching { loadStateNative(gameId, slot, path) }.getOrDefault(false)

    fun setCpuCycles(cycles: Int) {
        if (nativeLoaded) runCatching { setCpuCyclesNative(cycles) }
    }

    fun setFrameCap(fps: Int) {
        if (nativeLoaded) runCatching { setFrameCapNative(fps) }
    }

    fun notifyThermalLevel(level: Int) {
        if (nativeLoaded) runCatching { notifyThermalLevelNative(level) }
    }

    fun getPerfStats(): PerfStats {
        if (!nativeLoaded) return PerfStats(fps = 0f, cpuUsagePercent = 0f)
        val arr = runCatching { getPerfStatsNative() }.getOrDefault(floatArrayOf(0f, 0f))
        return PerfStats(fps = arr.getOrElse(0) { 0f }, cpuUsagePercent = arr.getOrElse(1) { 0f })
    }

    private external fun startDosboxNative(gameDirectory: String, configPath: String): Boolean
    private external fun stopDosboxNative()
    private external fun setVolumeNative(volume: Float)
    private external fun submitAudioPcm16Native(buffer: ShortArray, frames: Int, channels: Int)
    private external fun saveStateNative(gameId: String, slot: Int, path: String): Boolean
    private external fun loadStateNative(gameId: String, slot: Int, path: String): Boolean
    private external fun setCpuCyclesNative(cycles: Int)
    private external fun setFrameCapNative(fps: Int)
    private external fun notifyThermalLevelNative(level: Int)
    private external fun getPerfStatsNative(): FloatArray
}
