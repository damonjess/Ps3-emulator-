package com.retrorts.ui

import com.retrorts.SettingsState
import java.io.File

object NativeEmulatorBridge {
    private val nativeLoaded = runCatching {
        System.loadLibrary("pcsx_rearmed")
    }.isSuccess

    data class LaunchResult(val started: Boolean, val message: String)

    fun launchGame(context: android.content.Context, console: String, romPath: String, settings: SettingsState): LaunchResult {
        if (!nativeLoaded) return LaunchResult(false, "Native library unavailable")
        return runCatching {
            val cacheDir = context.cacheDir.absolutePath
            val saveDir = File(context.getExternalFilesDir(null), "Saves/$console").also { it.mkdirs() }.absolutePath
            val msg = launchGameNative(console, romPath, cacheDir, saveDir)
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

    fun stopGame() {
        if (nativeLoaded) runCatching { stopGameNative() }
    }

    fun updateInput(padIndex: Int, buttonMask: Int) {
        if (nativeLoaded) updateInputNative(padIndex, buttonMask)
    }

    private external fun launchGameNative(console: String, romPath: String, cacheDir: String, saveDir: String): String
    private external fun setSurfaceNative(surface: android.view.Surface?)
    private external fun stopGameNative()
    private external fun updateInputNative(padIndex: Int, buttonMask: Int)
}
