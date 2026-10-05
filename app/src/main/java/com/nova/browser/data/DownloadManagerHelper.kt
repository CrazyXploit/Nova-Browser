package com.nova.browser.data

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import androidx.core.content.FileProvider
import java.io.File

object DownloadManagerHelper {

    fun enqueue(
        context: Context,
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
    ): Long {
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(fileName)
            setDescription("Downloading…")
            setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
            userAgent?.let { addRequestHeader("User-Agent", it) }
            CookieManager.getInstance().getCookie(url)?.let {
                addRequestHeader("Cookie", it)
            }
        }
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return try {
            dm.enqueue(request)
        } catch (_: Throwable) { -1L }
    }

    data class DownloadStatus(
        val downloadedBytes: Long,
        val totalBytes: Long,
        val status: String,
        val localUri: String?,
    )

    fun query(context: Context, id: Long): DownloadStatus? {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = try {
            dm.query(DownloadManager.Query().setFilterById(id))
        } catch (_: Exception) { return null }

        cursor.use { c ->
            if (c == null || !c.moveToFirst()) return null
            val downloaded = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val statusInt = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val localUri = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))

            val statusText = when (statusInt) {
                DownloadManager.STATUS_PENDING -> "QUEUED"
                DownloadManager.STATUS_RUNNING -> "DOWNLOADING"
                DownloadManager.STATUS_PAUSED -> "PAUSED"
                DownloadManager.STATUS_SUCCESSFUL -> "COMPLETE"
                DownloadManager.STATUS_FAILED -> "FAILED"
                else -> "UNKNOWN"
            }
            return DownloadStatus(downloaded, total, statusText, localUri)
        }
    }

    fun openFile(context: Context, fileName: String) {
        try {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName,
            )
            if (!file.exists()) return

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            val mime = context.contentResolver.getType(uri) ?: "*/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
        } catch (_: Exception) {
        }
    }
}