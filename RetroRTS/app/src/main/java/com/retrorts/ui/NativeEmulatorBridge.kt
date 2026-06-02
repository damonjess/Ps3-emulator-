package com.retrorts.ui

object NativeEmulatorBridge {
    private val nativeLoaded = runCatching {
        System.loadLibrary("retrorts_dosbox_jni")
    }.isSuccess

    fun launchGame(console: String, romPath: String): String =
        if (nativeLoaded) {
            runCatching { launchGameNative(console, romPath) }.getOrElse { "ERROR: ${it.message ?: "native launch failed"}" }
        } else {
            "ERROR: native library retrorts_dosbox_jni was not loaded"
        }

    private external fun launchGameNative(console: String, romPath: String): String
}
