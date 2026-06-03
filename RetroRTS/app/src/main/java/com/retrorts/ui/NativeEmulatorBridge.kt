package com.retrorts.ui

import com.retrorts.SettingsState

object NativeEmulatorBridge {
    private val nativeLoaded = runCatching {
        System.loadLibrary("retrorts_jni")
    }.isSuccess

    data class LaunchResult(val started: Boolean, val message: String)

    fun launchGame(console: String, romPath: String, settings: SettingsState): LaunchResult {
        if (!nativeLoaded) return LaunchResult(false, "Native library unavailable")
        return runCatching {
            val msg = launchGameNative(console, romPath)
            // C++ returns "OK:..." for success or "ERR:..." for failure
            LaunchResult(
                started = msg.startsWith("OK") || msg.contains("ready"),
                message = msg
            )
        }.getOrElse { LaunchResult(false, "Crash in native: ${it.message}") }
    }

    fun setSurface(surface: android.view.Surface?) {
        if (nativeLoaded) runCatching { setSurfaceNative(surface) }
    }

    private external fun launchGameNative(console: String, romPath: String): String
    private external fun setSurfaceNative(surface: android.view.Surface?)
}
