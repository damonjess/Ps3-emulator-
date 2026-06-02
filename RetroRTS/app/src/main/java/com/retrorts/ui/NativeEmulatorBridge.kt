package com.retrorts.ui

object NativeEmulatorBridge {
    init {
        System.loadLibrary("retrorts_dosbox_jni")
    }

    external fun launchGame(console: String, romPath: String): String
}
