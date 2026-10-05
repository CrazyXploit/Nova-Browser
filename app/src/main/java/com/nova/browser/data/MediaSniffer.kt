package com.nova.browser.data

import android.net.Uri
import java.util.concurrent.ConcurrentHashMap

/**
 * Sniffs media files (.mp4, .m3u8, .mp3, .webm, .jpg, .png, .gif, .pdf) from network traffic.
 * Media list is per-page, cleared on navigation.
 */
object MediaSniffer {

    enum class MediaKind { VIDEO, AUDIO, IMAGE, DOCUMENT, STREAM, OTHER }

    data class MediaItem(
        val url: String,
        val kind: MediaKind,
        val sizeBytes: Long = 0,
        val contentType: String? = null,
    ) {
        val fileName: String get() = url.substringAfterLast('/').substringBefore('?')
        val displaySize: String
            get() = if (sizeBytes <= 0) "Unknown" else formatBytes(sizeBytes)
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
     * Called from shouldInterceptRequest. Returns true if the URL looks like downloadable media.
     */
    fun sniff(url: String, contentType: String?, contentLength: Long): Boolean {
        if (!enabled) return false

        val kind = detectKind(url, contentType) ?: return false

        // Skip tiny files
        if (contentLength in 1 until 10_000) return false

        // Skip if already tracked
        if (items.containsKey(url)) return false

        items[url] = MediaItem(
            url = url,
            kind = kind,
            sizeBytes = contentLength,
            contentType = contentType,
        )
        return true
    }

    fun list(): List<MediaItem> = items.values.sortedByDescending { it.sizeBytes }

    fun remove(url: String) {
        items.remove(url)
    }

    private fun detectKind(url: String, contentType: String?): MediaKind? {
        val lower = url.lowercase()
        val ct = contentType?.lowercase() ?: ""

        // Check content type first
        when {
            ct.startsWith("video/") -> return MediaKind.VIDEO
            ct.startsWith("audio/") -> return MediaKind.AUDIO
            ct.startsWith("image/") -> return MediaKind.IMAGE
        }

        // Check extension
        return when {
            lower.endsWith(".mp4") ||
                lower.endsWith(".webm") ||
                lower.endsWith(".mkv") ||
                lower.endsWith(".mov") -> MediaKind.VIDEO

            lower.endsWith(".m3u8") ||
                lower.endsWith(".mpd") ||
                lower.endsWith(".ts") -> MediaKind.STREAM

            lower.endsWith(".mp3") ||
                lower.endsWith(".m4a") ||
                lower.endsWith(".aac") ||
                lower.endsWith(".ogg") ||
                lower.endsWith(".wav") -> MediaKind.AUDIO

            lower.endsWith(".pdf") -> MediaKind.DOCUMENT

            lower.endsWith(".jpg") ||
                lower.endsWith(".jpeg") ||
                lower.endsWith(".png") ||
                lower.endsWith(".gif") ||
                lower.endsWith(".webp") ||
                lower.endsWith(".svg") -> MediaKind.IMAGE

            else -> null
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "Unknown"
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
}