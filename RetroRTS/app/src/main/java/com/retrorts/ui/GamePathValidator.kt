package com.retrorts.ui

object GamePathValidator {
    fun isValid(path: String): Boolean {
        if (path.isBlank()) return false
        val n = path.lowercase()
        return n.startsWith("/sdcard/")
                || n.startsWith("/storage/emulated/")
                || n.startsWith("content://")
    }
}
