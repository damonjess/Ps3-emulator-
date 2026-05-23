package com.example.ps3emu.core

class NativePs3Core : EmulatorCore {
    external override fun start(console: ConsoleType, gamePath: String?)
    external override fun stop()
    external override fun pause()
    external override fun resume()
    external override fun onButtonChanged(button: GamepadButton, pressed: Boolean)
    external override fun onStickChanged(stick: AnalogStick, state: StickState)
    external override fun getLatestFrameInfo(): FrameInfo

    companion object {
        init {
            runCatching { System.loadLibrary("psxcore") }
        }
    }
}
