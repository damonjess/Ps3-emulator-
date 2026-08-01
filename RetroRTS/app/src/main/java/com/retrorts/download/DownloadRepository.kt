package com.retrorts.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.getSystemService

object DownloadRepository {

    /**
     * Test suites and homebrew games for RetroRTS.
     */
    fun getAvailableSuites(): List<DownloadableSuite> = listOf(
        DownloadableSuite(
            id = "240p_ps1",
            name = "240p Test Suite (PS1)",
            description = "Essential video diagnostic tool for PlayStation 1.",
            platform = "PS1",
            downloadUrl = "https://github.com/filipalac/240pTestSuite-PS1/releases/download/v1.17/240pTestSuitePS1.zip",
            fileName = "240pTestSuitePS1.zip",
            installSubPath = "PS1"
        ),
        DownloadableSuite(
            id = "celeste_ps1",
            name = "Celeste Classic (PS1)",
            description = "Faithful PS1 port of the original Pico-8 Celeste.",
            platform = "PS1",
            downloadUrl = "https://github.com/wildmonkeydan/ccleste-psx/releases/download/v1.1/build.zip",
            fileName = "celeste_classic_ps1.zip",
            installSubPath = "PS1"
        ),
        DownloadableSuite(
            id = "freedos_13",
            name = "FreeDOS 1.3 Floppy",
            description = "Legal and free DOS operating system (Floppy Edition).",
            platform = "DOSBOX",
            downloadUrl = "https://www.freedos.org/download/download/FD13-FLOPPY.zip",
            fileName = "FD13-FLOPPY.zip",
            installSubPath = "DOSBox"
        ),
        DownloadableSuite(
            id = "amiga_testkit",
            name = "Amiga Test Kit",
            description = "Diagnostic tool for Amiga hardware.",
            platform = "AMIGA",
            downloadUrl = "https://github.com/keirf/Amiga-Test-Kit/releases/download/v1.20/AmigaTestKit-v1.20.zip",
            fileName = "AmigaTestKit.zip",
            installSubPath = "Amiga"
        )
    )

    fun startDownload(context: Context, suite: DownloadableSuite): Long {
        val dm = context.getSystemService<DownloadManager>() ?: return -1L

        val request = DownloadManager.Request(Uri.parse(suite.downloadUrl))
            .setTitle("RetroRTS: ${suite.name}")
            .setDescription("Downloading ${suite.fileName}…")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "retrorts_${suite.fileName}")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        return dm.enqueue(request)
    }

    fun queryStatus(context: Context, downloadId: Long): Int {
        val dm = context.getSystemService<DownloadManager>() ?: return -1
        val query = DownloadManager.Query().setFilterById(downloadId)
        dm.query(query)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                if (statusIdx >= 0) return cursor.getInt(statusIdx)
            }
        }
        return -1
    }
}
