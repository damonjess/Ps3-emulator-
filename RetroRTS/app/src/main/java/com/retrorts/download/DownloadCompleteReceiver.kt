package com.retrorts.download

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.getSystemService
import java.io.File

class DownloadCompleteReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "RetroRTS_Download"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return

        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (downloadId == -1L) return

        val dm = context.getSystemService<DownloadManager>() ?: return
        val query = DownloadManager.Query().setFilterById(downloadId)
        dm.query(query)?.use { cursor ->
            if (!cursor.moveToFirst()) return

            val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val uriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            if (statusIdx < 0 || uriIdx < 0) return

            val status = cursor.getInt(statusIdx)
            if (status != DownloadManager.STATUS_SUCCESSFUL) {
                val reasonIdx = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                val reason = if (reasonIdx >= 0) cursor.getInt(reasonIdx) else -1
                val message = when (reason) {
                    DownloadManager.ERROR_CANNOT_RESUME -> "Cannot resume download."
                    DownloadManager.ERROR_DEVICE_NOT_FOUND -> "SD card not found."
                    DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "File already exists."
                    DownloadManager.ERROR_FILE_ERROR -> "Local file error."
                    DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP data error."
                    DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Storage full."
                    DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many redirects."
                    DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Unhandled HTTP code."
                    DownloadManager.ERROR_UNKNOWN -> "Unknown network error."
                    404 -> "File not found (404)."
                    500 -> "Server error (500)."
                    else -> "Download failed (Code $reason)."
                }
                Log.e(TAG, "Download $downloadId failed: $message")
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                return
            }

            val localUri = cursor.getString(uriIdx) ?: return
            val downloadedFile = File(Uri.parse(localUri).path ?: return)

            if (!downloadedFile.exists()) {
                Log.e(TAG, "Downloaded file not found at $localUri")
                return
            }

            // Determine target directory based on filename prefix or mapping
            val targetDir = resolveTargetDirectory(downloadedFile.name)
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                Log.e(TAG, "Failed to create target directory: ${targetDir.absolutePath}")
                Toast.makeText(context, "Install failed: Cannot create folder on SD card. check permissions.", Toast.LENGTH_LONG).show()
                return
            }

            val destFile = File(targetDir, stripPrefix(downloadedFile.name))

            try {
                downloadedFile.inputStream().use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.i(TAG, "Auto-installed ${destFile.name} to ${destFile.absolutePath}")
                Toast.makeText(context, "Installed: ${destFile.name}", Toast.LENGTH_LONG).show()

                // Optional: delete from app-specific Downloads folder after successful copy
                downloadedFile.delete()

            } catch (e: Exception) {
                Log.e(TAG, "Auto-install failed", e)
                val msg = if (e.message?.contains("Permission denied") == true) {
                    "Install failed: Permission denied. Please grant 'All Files Access'."
                } else {
                    "Install failed: ${e.message}"
                }
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun resolveTargetDirectory(fileName: String): File {
        val sdcard = Environment.getExternalStorageDirectory()
        val base = File(sdcard, "RetroRTS/Games")

        return when {
            fileName.contains("ps1", ignoreCase = true) -> File(base, "PS1")
            fileName.contains("amiga", ignoreCase = true) -> File(base, "Amiga")
            fileName.contains("dosbox", ignoreCase = true) || fileName.contains("dos", ignoreCase = true) -> File(base, "DOSBox")
            fileName.contains("dsi", ignoreCase = true) || fileName.contains("nds", ignoreCase = true) -> File(base, "NintendoDSi")
            else -> File(base, "Downloads")
        }
    }

    private fun stripPrefix(name: String): String {
        return if (name.startsWith("retrorts_")) name.removePrefix("retrorts_") else name
    }
}
