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
        external fun setCpuCycles(cycles: Int)

        @JvmStatic
        external fun setFrameCap(fps: Int)

        @JvmStatic
        external fun setVolume(volume: Float)

        @JvmStatic
        fun stopDosbox() = stopDosboxNative()

        @JvmStatic
        fun notifyThermalLevel(level: Int) {
            // Optional: Implement native hook if C++ side supports it
        }
    }
}
