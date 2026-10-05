package com.nova.browser.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: Long = 0,
    val url: String,
    val fileName: String,
    val mimeType: String?,
    val contentLength: Long,
    val downloadedBytes: Long = 0,
    val status: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis(),
)