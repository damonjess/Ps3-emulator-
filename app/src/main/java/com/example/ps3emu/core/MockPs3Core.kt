package com.example.ps3emu.core

import android.util.Log

class MockPs3Core : EmulatorCore {
    private val tag = "MockPsCore"
    private var running = false
    private var activeConsole: ConsoleType = ConsoleType.PS2

    override fun start(console: ConsoleType, gamePath: String?) {
        activeConsole = console
        running = true
        Log.d(tag, "Core started. console=$console gamePath=$gamePath")
    }

    override fun stop() {
        running = false
        Log.d(tag, "Core stopped")
    }

    override fun pause() {
        running = false
        Log.d(tag, "Core paused")
    }

    override fun resume() {
        running = true
        Log.d(tag, "Core resumed")
    }

    override fun onButtonChanged(button: GamepadButton, pressed: Boolean) {
        Log.d(tag, "Input: $button = $pressed")
    }

    override fun onStickChanged(stick: AnalogStick, state: StickState) {
        Log.d(tag, "Stick: $stick = (${state.x}, ${state.y})")
    }

    override fun getLatestFrameInfo(): FrameInfo {
        val status = if (running) "Running ${activeConsole.name} (mock)" else "Idle ${activeConsole.name} (mock)"
        val resolution = if (activeConsole == ConsoleType.PS1) 480 to 272 else 640 to 448
        return FrameInfo(width = resolution.first, height = resolution.second, status = status)
    }
}
