package com.retrorts.ui

class DosboxBridge {
    companion object {
        init {
            System.loadLibrary("retrorts_dosbox_jni")
        }

        @JvmStatic
        external fun startDosboxNative(gameDir: String, configPath: String): Boolean

        @JvmStatic
        external fun stopDosboxNative()

        @JvmStatic
        private external fun setCpuCyclesNative(cycles: Int)

        @JvmStatic
        private external fun setFrameCapNative(fps: Int)

        @JvmStatic
        private external fun setVolumeNative(volume: Float)

        @JvmStatic
        private external fun notifyThermalLevelNative(level: Int)

        @JvmStatic
        external fun getPerfStatsNative(): FloatArray

        @JvmStatic
        fun stopDosbox() = stopDosboxNative()

        @JvmStatic
        fun setCpuCycles(cycles: Int) = setCpuCyclesNative(cycles)

        @JvmStatic
        fun setFrameCap(fps: Int) = setFrameCapNative(fps)

        @JvmStatic
        fun setVolume(volume: Float) = setVolumeNative(volume)

        @JvmStatic
        fun notifyThermalLevel(level: Int) = notifyThermalLevelNative(level)
    }
}
