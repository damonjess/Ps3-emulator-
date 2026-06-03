package com.retrorts.ui

object NativeEmulatorBridge {
    private val nativeLoaded = runCatching {
        System.loadLibrary("retrorts_jni")
    }.isSuccess

    fun launchGame(console: String, romPath: String): String =
        if (nativeLoaded) {
            runCatching { launchGameNative(console, romPath) }.getOrElse { "ERROR: ${it.message ?: "native launch failed"}" }
        } else {
            "ERROR: native library retrorts_jni was not loaded"
        }

    fun setSurface(surface: android.view.Surface?) {
        if (nativeLoaded) runCatching { setSurfaceNative(surface) }
    }

    private external fun launchGameNative(console: String, romPath: String): String
    private external fun setSurfaceNative(surface: android.view.Surface?)
}
