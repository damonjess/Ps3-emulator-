package com.retrorts.ui

object NativeEmulatorBridge {
    init {
        System.loadLibrary("retrorts_core")
    }

    external fun launchGame(console: String, romPath: String): String
}
