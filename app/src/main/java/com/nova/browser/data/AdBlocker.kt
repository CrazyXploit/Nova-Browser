package com.nova.browser.data

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object AdBlocker {

    private val blockedHosts = hashSetOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "google-analytics.com", "googletagmanager.com", "googletagservices.com",
        "adservice.google.com", "ads.google.com", "pagead2.googlesyndication.com",
        "partner.googleadservices.com", "adnxs.com", "ads.yahoo.com",
        "advertising.com", "amazon-adsystem.com", "facebook.net", "fbcdn.net",
        "scorecardresearch.com", "outbrain.com", "taboola.com", "criteo.com",
        "criteo.net", "pubmatic.com", "rubiconproject.com", "openx.net",
        "casalemedia.com", "smartadserver.com", "sharethrough.com", "teads.tv",
        "adform.net", "bidswitch.net", "adroll.com", "quantserve.com",
        "hotjar.com", "mixpanel.com", "segment.io", "segment.com",
        "amplitude.com", "branch.io", "appsflyer.com", "adjust.com",
    )

    private val blockedKeywords = arrayOf(
        "/ads/", "/ad/", "/adserver/", "/advert/",
        "/banner/", "/popup/", "/tracking/", "/tracker/",
        "/analytics/", "/pixel/", "/beacon/",
    )

    /** Hot cache — recently checked URLs. Avoids re-scanning. */
    private val decisionCache = ConcurrentHashMap<String, Boolean>(512)

    private val emptyResponse: WebResourceResponse by lazy {
        WebResourceResponse(
            "text/plain",
            "utf-8",
            ByteArrayInputStream(ByteArray(0)),
        )
    }

    @Volatile var enabled: Boolean = true
    @Volatile var trackersBlocked: Int = 0
    @Volatile var adsBlocked: Int = 0

    private val _trackerCount = AtomicInteger(0)
    private val _adCount = AtomicInteger(0)

    fun shouldBlock(url: String?): Boolean {
        if (!enabled || url.isNullOrEmpty()) return false

        // Hot cache hit
        decisionCache[url]?.let { return it }

        val lower = url.lowercase()
        var block = false

        for (host in blockedHosts) {
            if (lower.contains(host)) {
                trackersBlocked = _trackerCount.incrementAndGet()
                DataSaver.onBlockedResource()
                block = true
                break
            }
        }

        if (!block) {
            for (kw in blockedKeywords) {
                if (lower.contains(kw)) {
                    adsBlocked = _adCount.incrementAndGet()
                    DataSaver.onBlockedResource()
                    block = true
                    break
                }
            }
        }

        // Cache decision
        if (decisionCache.size < 500) {
            decisionCache[url] = block
        }
        return block
    }

    fun blockedResponse(): WebResourceResponse = emptyResponse

    fun totalBlocked(): Int = _trackerCount.get() + _adCount.get()

    fun resetCounters() {
        _trackerCount.set(0)
        _adCount.set(0)
        trackersBlocked = 0
        adsBlocked = 0
        decisionCache.clear()
    }
}