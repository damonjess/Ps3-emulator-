package com.retrorts.ui

object GamePathValidator {
    fun isValid(path: String): Boolean {
        if (path.isBlank()) return false
        val normalized = path.lowercase()
        return normalized.startsWith("/sdcard/") || normalized.startsWith("content://")
    }
}
