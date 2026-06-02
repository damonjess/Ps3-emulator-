package com.example.ps3emu.core

enum class ConsoleType { PS1, PS2 }

enum class GamepadButton {
    UP, DOWN, LEFT, RIGHT,
    TRIANGLE, CIRCLE, CROSS, SQUARE,
    L1, R1, L2, R2,
    L3, R3,
    START, SELECT, PS
}

enum class AnalogStick { LEFT, RIGHT }

data class StickState(val x: Float, val y: Float)

data class FrameInfo(
    val width: Int,
    val height: Int,
    val status: String,
)

interface EmulatorCore {
    fun start(console: ConsoleType, gamePath: String? = null)
    fun stop()
    fun pause()
    fun resume()

    fun onButtonChanged(button: GamepadButton, pressed: Boolean)
    fun onStickChanged(stick: AnalogStick, state: StickState)

    fun getLatestFrameInfo(): FrameInfo
}
