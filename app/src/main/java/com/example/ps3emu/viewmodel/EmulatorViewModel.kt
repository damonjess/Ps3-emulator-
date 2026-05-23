package com.example.ps3emu.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ps3emu.core.AnalogStick
import com.example.ps3emu.core.ConsoleType
import com.example.ps3emu.core.EmulatorCore
import com.example.ps3emu.core.FrameInfo
import com.example.ps3emu.core.GamepadButton
import com.example.ps3emu.core.StickState

class EmulatorViewModel(
    private val core: EmulatorCore
) {
    var statusText by mutableStateOf("Idle")
        private set

    var selectedConsole by mutableStateOf(ConsoleType.PS2)
        private set

    var frameInfo by mutableStateOf(FrameInfo(0, 0, "Not started"))
        private set

    fun setConsole(consoleType: ConsoleType) {
        selectedConsole = consoleType
    }

    fun start(gamePath: String? = null) {
        core.start(selectedConsole, gamePath)
        frameInfo = core.getLatestFrameInfo()
        statusText = frameInfo.status
    }

    fun stop() {
        core.stop()
        frameInfo = core.getLatestFrameInfo()
        statusText = "Stopped"
    }

    fun pause() {
        core.pause()
        frameInfo = core.getLatestFrameInfo()
        statusText = "Paused"
    }

    fun resume() {
        core.resume()
        frameInfo = core.getLatestFrameInfo()
        statusText = frameInfo.status
    }

    fun onButton(button: GamepadButton, pressed: Boolean) {
        core.onButtonChanged(button, pressed)
    }

    fun onStick(stick: AnalogStick, x: Float, y: Float) {
        core.onStickChanged(stick, StickState(x, y))
    }
}
