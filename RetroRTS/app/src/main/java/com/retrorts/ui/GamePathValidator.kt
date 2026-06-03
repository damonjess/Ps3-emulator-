package com.retrorts.ui

object GamePathValidator {
    fun isValid(path: String): Boolean {
        if (path.isBlank()) return false
        val n = path.lowercase()

        val validRoot = n.startsWith("/sdcard/")
                || n.startsWith("/storage/emulated/")
                || n.startsWith("content://")

        if (!validRoot) return false

        // DOS / Amiga / DSi types already supported
        val knownExtensions = listOf(
            ".exe", ".com", ".bat", ".conf",   // DOSBox
            ".adf", ".hdf", ".dms",            // Amiga
            ".nds", ".dsi", ".srl",            // Nintendo DS/DSi
            // PS1 disc images
            ".bin", ".cue", ".img", ".iso",
            // folder-based game (no extension)
            ""
        )

        val ext = if (n.contains('.')) n.substringAfterLast('.').let { ".$it" } else ""
        return ext in knownExtensions || n.endsWith("/")
    }
}
