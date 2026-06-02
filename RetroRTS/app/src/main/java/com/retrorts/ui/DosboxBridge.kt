package com.retrorts.ui

data class PerfStats(
    val fps: Float,
    val cpuUsagePercent: Float,
)

object DosboxBridge {
    init {
        System.loadLibrary("retrorts_dosbox_jni")
    }

    fun startDosbox(gameDirectory: String, configPath: String): Boolean = startDosboxNative(gameDirectory, configPath)
    fun stopDosbox() = stopDosboxNative()
    fun setVolume(volume: Float) = setVolumeNative(volume)
    fun submitAudioPcm16(buffer: ShortArray, frames: Int, channels: Int) = submitAudioPcm16Native(buffer, frames, channels)
    fun saveState(gameId: String, slot: Int, path: String): Boolean = saveStateNative(gameId, slot, path)
    fun loadState(gameId: String, slot: Int, path: String): Boolean = loadStateNative(gameId, slot, path)

    fun setCpuCycles(cycles: Int) = setCpuCyclesNative(cycles)
    fun setFrameCap(fps: Int) = setFrameCapNative(fps)
    fun notifyThermalLevel(level: Int) = notifyThermalLevelNative(level)
    fun getPerfStats(): PerfStats {
        val arr = getPerfStatsNative()
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
