package com.nova.browser.data

import java.util.concurrent.ConcurrentHashMap

/**
 * Sniffs media files from network traffic with rich metadata.
 * Detects: images, GIFs, videos, audio, streams, documents.
 * Duration/dimensions are filled asynchronously after sniffing.
 */
object MediaSniffer {

    enum class MediaKind { IMAGE, GIF, VIDEO, AUDIO, STREAM, DOCUMENT, OTHER }

    data class MediaItem(
        val url: String,
        val kind: MediaKind,
        var sizeBytes: Long = 0,
        var contentType: String? = null,
        var durationMs: Long = 0,
        var widthPx: Int = 0,
        var heightPx: Int = 0,
    ) {
        val fileName: String
            get() = url.substringAfterLast('/').substringBefore('?').ifBlank { "media" }

        val displaySize: String
            get() = if (sizeBytes <= 0) "—" else formatBytes(sizeBytes)

        val displayDuration: String
            get() = if (durationMs <= 0) "" else formatDuration(durationMs)

        val displayDimensions: String
            get() = if (widthPx > 0 && heightPx > 0) "${widthPx}×${heightPx}" else ""

        val kindLabel: String
            get() = when (kind) {
                MediaKind.IMAGE -> "Image"
                MediaKind.GIF -> "GIF"
                MediaKind.VIDEO -> "Video"
                MediaKind.AUDIO -> "Audio"
                MediaKind.STREAM -> "Stream"
                MediaKind.DOCUMENT -> "Document"
                MediaKind.OTHER -> "File"
            }
    }

    private val items = ConcurrentHashMap<String, MediaItem>()
    private var currentPageHost: String = ""

    @Volatile var enabled: Boolean = true

    fun reset() {
        items.clear()
        currentPageHost = ""
    }

    fun setPageHost(host: String) {
        if (currentPageHost != host) {
            currentPageHost = host
            items.clear()
        }
    }

    /**
     * Returns true if the URL/type is media-worthy and was added.
     */
    fun sniff(url: String, contentType: String?, contentLength: Long): Boolean {
        if (!enabled) return false

        val kind = detectKind(url, contentType) ?: return false

        if (contentLength in 1 until 5_000) return false

        if (items.containsKey(url)) return false

        items[url] = MediaItem(
            url = url,
            kind = kind,
            sizeBytes = contentLength,
            contentType = contentType,
        )
        return true
    }

    /**
     * Update size after download (if unknown at sniff time).
     */
    fun updateSize(url: String, sizeBytes: Long) {
        items[url]?.let { it.sizeBytes = sizeBytes }
    }

    /**
     * Update media metadata (duration, dimensions).
     */
    fun updateMetadata(
        url: String,
        durationMs: Long = 0,
        widthPx: Int = 0,
        heightPx: Int = 0,
    ) {
        items[url]?.let {
            if (durationMs > 0) it.durationMs = durationMs
            if (widthPx > 0) it.widthPx = widthPx
            if (heightPx > 0) it.heightPx = heightPx
        }
    }

    fun list(): List<MediaItem> = items.values.sortedByDescending { it.sizeBytes }

    fun listFiltered(filter: MediaKind?): List<MediaItem> =
        if (filter == null) list()
        else items.values.filter { it.kind == filter }.sortedByDescending { it.sizeBytes }

    fun remove(url: String) {
        items.remove(url)
    }

    fun countByKind(kind: MediaKind): Int = items.values.count { it.kind == kind }

    private fun detectKind(url: String, contentType: String?): MediaKind? {
        val lower = url.lowercase().substringBefore('?').substringBefore('#')
        val ct = contentType?.lowercase() ?: ""

        // GIF — separate kind
        if (lower.endsWith(".gif") || ct == "image/gif") return MediaKind.GIF

        when {
            ct.startsWith("video/") -> return MediaKind.VIDEO
            ct.startsWith("audio/") -> return MediaKind.AUDIO
            ct.startsWith("image/") -> return MediaKind.IMAGE
            ct == "application/pdf" -> return MediaKind.DOCUMENT
            ct == "application/x-mpegurl" ||
                ct == "application/vnd.apple.mpegurl" -> return MediaKind.STREAM
            ct == "application/dash+xml" -> return MediaKind.STREAM
        }

        return when {
            lower.endsWith(".mp4") ||
                lower.endsWith(".webm") ||
                lower.endsWith(".mkv") ||
                lower.endsWith(".mov") ||
                lower.endsWith(".m4v") -> MediaKind.VIDEO

            lower.endsWith(".m3u8") ||
                lower.endsWith(".mpd") -> MediaKind.STREAM

            lower.endsWith(".mp3") ||
                lower.endsWith(".m4a") ||
                lower.endsWith(".aac") ||
                lower.endsWith(".ogg") ||
                lower.endsWith(".opus") ||
                lower.endsWith(".wav") ||
                lower.endsWith(".flac") -> MediaKind.AUDIO

            lower.endsWith(".pdf") -> MediaKind.DOCUMENT

            lower.endsWith(".jpg") ||
                lower.endsWith(".jpeg") ||
                lower.endsWith(".png") ||
                lower.endsWith(".webp") ||
                lower.endsWith(".svg") ||
                lower.endsWith(".avif") -> MediaKind.IMAGE

            else -> null
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "—"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1 -> "%.2f GB".format(gb)
            mb >= 1 -> "%.1f MB".format(mb)
            kb >= 1 -> "%.0f KB".format(kb)
            else -> "$bytes B"
        }
    }

    private fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return when {
            h > 0 -> "%d:%02d:%02d".format(h, m, s)
            else -> "%d:%02d".format(m, s)
        }
    }
}