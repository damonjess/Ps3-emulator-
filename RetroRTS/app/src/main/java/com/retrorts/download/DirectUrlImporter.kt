package com.retrorts.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import java.io.File

/**
 * DirectUrlImporter lets users paste a direct download link.
 * The app downloads the file and auto-installs it to the correct RetroRTS/Games folder
 * based on the file extension.
 *
 * DISCLAIMER: Users must only download files they have the legal right to obtain.
 */
object DirectUrlImporter {

    private const val TAG = "RetroRTS_Importer"

    fun enqueueDownload(context: Context, url: String, customFileName: String? = null): Long {
        if (url.isBlank()) return -1L

        val uri = url.toUri()
        val fileName = customFileName ?: uri.lastPathSegment ?: "download_${System.currentTimeMillis()}"
        val sanitized = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")

        val request = DownloadManager.Request(uri)
            .setTitle("Importing: $sanitized")
            .setDescription("RetroRTS is downloading your file…")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "retrorts_import_$sanitized")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = context.getSystemService<DownloadManager>() ?: return -1L
        return dm.enqueue(request)
    }

    /**
     * Call this from your DownloadCompleteReceiver to handle direct-import downloads.
     * Returns true if this download ID was handled by the importer.
     */
    fun handleCompletedDownload(context: Context, downloadId: Long): Boolean {
        val dm = context.getSystemService<DownloadManager>() ?: return false
        val query = DownloadManager.Query().setFilterById(downloadId)
        dm.query(query)?.use { cursor ->
            if (!cursor.moveToFirst()) return false

            val uriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            if (uriIdx < 0 || statusIdx < 0) return false

            if (cursor.getInt(statusIdx) != DownloadManager.STATUS_SUCCESSFUL) return false

            val localUri = cursor.getString(uriIdx) ?: return false
            val downloadedFile = File(Uri.parse(localUri).path ?: return false)

            if (!downloadedFile.name.startsWith("retrorts_import_")) return false

            val targetDir = resolveGamesDirectory(downloadedFile.name)
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                Log.e(TAG, "Failed to create directory: ${targetDir.absolutePath}")
            }

            val destFile = File(targetDir, stripImportPrefix(downloadedFile.name))

            return try {
                downloadedFile.inputStream().use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.i(TAG, "Installed ${destFile.name} to ${destFile.absolutePath}")
                Toast.makeText(context, "Installed to ${destFile.name}", Toast.LENGTH_LONG).show()
                downloadedFile.delete() // clean up Downloads folder
                true
            } catch (e: Exception) {
                Log.e(TAG, "Install failed", e)
                Toast.makeText(context, "Install failed: ${e.message}", Toast.LENGTH_LONG).show()
                false
            }
        }
        return false
    }

    private fun resolveGamesDirectory(fileName: String): File {
        val sdcard = Environment.getExternalStorageDirectory()
        val base = File(sdcard, "RetroRTS/Games")
        val lower = fileName.lowercase()

        return when {
            lower.endsWith(".bin") || lower.endsWith(".cue") ||
            lower.endsWith(".img") || lower.endsWith(".iso") ||
            lower.endsWith(".pbp") || lower.endsWith(".chd") -> File(base, "PS1")

            lower.endsWith(".adf") || lower.endsWith(".hdf") ||
            lower.endsWith(".dms") || lower.endsWith(".ipf") -> File(base, "Amiga")

            lower.endsWith(".exe") || lower.endsWith(".com") ||
            lower.endsWith(".bat") || lower.endsWith(".conf") ||
            (lower.endsWith(".zip") && (lower.contains("dos") || lower.contains("pc"))) -> File(base, "DOSBox")

            lower.endsWith(".nds") || lower.endsWith(".dsi") ||
            lower.endsWith(".srl") || lower.endsWith(".ids") -> File(base, "NintendoDSi")

            else -> File(base, "Imports")
        }
    }

    private fun stripImportPrefix(name: String): String {
        return if (name.startsWith("retrorts_import_")) name.removePrefix("retrorts_import_") else name
    }
}
