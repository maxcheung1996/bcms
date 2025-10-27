package com.socam.bcms.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

/**
 * ApkDownloader - Utility class for downloading and installing APK files
 * Handles the complete flow: download → install prompt
 */
class ApkDownloader(private val context: Context) {

    private var downloadId: Long = -1
    private var onDownloadComplete: ((Boolean, String?) -> Unit)? = null

    // BroadcastReceiver for download completion
    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) {
                handleDownloadComplete()
            }
        }
    }

    /**
     * Start downloading APK from URL
     */
    fun downloadApk(
        downloadUrl: String,
        fileName: String = "bcms_update.apk",
        onComplete: (Boolean, String?) -> Unit
    ): Unit {
        this.onDownloadComplete = onComplete

        try {
            // Register download completion receiver
            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            // For Android 13+ (API 33+), use RECEIVER_NOT_EXPORTED flag
            if (Build.VERSION.SDK_INT >= 33) { // TIRAMISU
                context.registerReceiver(downloadCompleteReceiver, filter, 2) // RECEIVER_NOT_EXPORTED
            } else {
                context.registerReceiver(downloadCompleteReceiver, filter)
            }

            // Create download request
            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle("BCMS App Update")
                .setDescription("Downloading new version...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            // Start download
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = downloadManager.enqueue(request)

            println("ApkDownloader: Download started with ID: $downloadId")

        } catch (e: Exception) {
            println("ApkDownloader: Error starting download: ${e.message}")
            onComplete(false, e.message)
            cleanup()
        }
    }

    /**
     * Handle download completion
     */
    private fun handleDownloadComplete(): Unit {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)

            if (cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val status = cursor.getInt(statusIndex)

                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        println("ApkDownloader: Download successful")
                        val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                        val fileUri = cursor.getString(uriIndex)
                        cursor.close()

                        // Trigger installation
                        installApk(fileUri)
                        onDownloadComplete?.invoke(true, null)
                    }
                    DownloadManager.STATUS_FAILED -> {
                        val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                        val reason = cursor.getInt(reasonIndex)
                        cursor.close()
                        println("ApkDownloader: Download failed with reason: $reason")
                        onDownloadComplete?.invoke(false, "Download failed (code: $reason)")
                    }
                    else -> {
                        cursor.close()
                        println("ApkDownloader: Download status: $status")
                    }
                }
            } else {
                cursor.close()
                onDownloadComplete?.invoke(false, "Download not found")
            }

        } catch (e: Exception) {
            println("ApkDownloader: Error handling download completion: ${e.message}")
            onDownloadComplete?.invoke(false, e.message)
        } finally {
            cleanup()
        }
    }

    /**
     * Install APK after download
     */
    private fun installApk(uriString: String): Unit {
        try {
            val fileUri = Uri.parse(uriString)
            val file = File(fileUri.path ?: return)

            if (!file.exists()) {
                println("ApkDownloader: Downloaded file not found: ${file.path}")
                return
            }

            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Use FileProvider for Android 7.0+
                val apkUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
            }

            context.startActivity(intent)
            println("ApkDownloader: Installation prompt opened")

        } catch (e: Exception) {
            println("ApkDownloader: Error installing APK: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Cleanup resources
     */
    private fun cleanup(): Unit {
        try {
            context.unregisterReceiver(downloadCompleteReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
            println("ApkDownloader: Cleanup - receiver not registered")
        }
    }

    /**
     * Cancel ongoing download
     */
    fun cancelDownload(): Unit {
        if (downloadId != -1L) {
            try {
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                downloadManager.remove(downloadId)
                println("ApkDownloader: Download cancelled")
            } catch (e: Exception) {
                println("ApkDownloader: Error cancelling download: ${e.message}")
            }
        }
        cleanup()
    }
}

