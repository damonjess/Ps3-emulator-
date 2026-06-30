package com.retrorts.ui

import android.view.Surface
import java.io.File

/**
 * AmigaBridge provides JNI bindings to the UAE (Universal Amiga Emulator) backend.
 * 
 * This bridge handles:
 * - Launching Amiga games from disk images (.adf, .hdf, .dms)
 * - Managing emulator lifecycle (start, stop, pause, resume)
 * - Input handling (joystick/keyboard)
 * - Surface rendering
 */
object AmigaBridge {
    private val nativeLoaded = runCatching {
        System.loadLibrary("pcsx_rearmed")  // Same library exports Amiga functions
    }.isSuccess

    data class LaunchResult(val started: Boolean, val message: String)

    /**
     * Start the Amiga emulator with a game disk image.
     * 
     * @param gamePath Full path to the Amiga game disk image (.adf, .hdf, .dms)
     * @param configPath Path to the generated UAE configuration file
     * @return true if emulator started successfully
     */
    fun startAmiga(gamePath: String, configPath: String): Boolean {
        if (!nativeLoaded) return false
        return runCatching {
            startAmigaNative(gamePath, configPath)
        }.getOrDefault(false)
    }

    /**
     * Stop the Amiga emulator.
     */
    fun stopAmiga() {
        if (nativeLoaded) runCatching { stopAmigaNative() }
    }

    /**
     * Check if the Amiga emulator is currently running.
     */
    fun isRunning(): Boolean {
        if (!nativeLoaded) return false
        return runCatching { isRunningNative() }.getOrDefault(false)
    }

    /**
     * Update input state for the emulator.
     * 
     * @param port Joystick port (0 or 1)
     * @param buttonMask Bitmask of pressed buttons
     */
    fun updateInput(port: Int, buttonMask: Int) {
        if (nativeLoaded) runCatching { updateInputNative(port, buttonMask) }
    }

    /**
     * Set the rendering surface for the emulator.
     * 
     * @param surface Android Surface for rendering output
     */
    fun setSurface(surface: Surface?) {
        if (nativeLoaded) runCatching { setSurfaceNative(surface) }
    }

    // JNI function declarations
    private external fun startAmigaNative(gamePath: String, configPath: String): Boolean
    private external fun stopAmigaNative()
    private external fun isRunningNative(): Boolean
    private external fun updateInputNative(port: Int, buttonMask: Int)
    private external fun setSurfaceNative(surface: Surface?)
}
