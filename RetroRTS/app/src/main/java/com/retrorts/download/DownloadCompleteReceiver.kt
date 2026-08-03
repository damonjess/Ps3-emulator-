package com.retrorts.download

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class DownloadCompleteReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "RetroRTS_Download"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return

        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (downloadId == -1L) return

        // Try the direct URL importer first
        val handled = DirectUrlImporter.handleCompletedDownload(context, downloadId)
        if (handled) {
            Log.i(TAG, "Direct import handled download $downloadId")
            return
        }

        // Fallback: handle legacy test-suite install logic if needed
        // (This is now redundant if we transition fully to DirectUrlImporter logic,
        // but kept here for structural integrity if you have other receivers)
    }
}
