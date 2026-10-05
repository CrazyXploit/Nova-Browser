package com.nova.browser.data

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
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
        val lower = url.lowercase()

        for (host in blockedHosts) {
            if (lower.contains(host)) {
                trackersBlocked = _trackerCount.incrementAndGet()
                DataSaver.onBlockedResource()
                return true
            }
        }
        for (kw in blockedKeywords) {
            if (lower.contains(kw)) {
                adsBlocked = _adCount.incrementAndGet()
                DataSaver.onBlockedResource()
                return true
            }
        }
        return false
    }

    fun blockedResponse(): WebResourceResponse = emptyResponse

    fun totalBlocked(): Int = _trackerCount.get() + _adCount.get()

    fun resetCounters() {
        _trackerCount.set(0)
        _adCount.set(0)
        trackersBlocked = 0
        adsBlocked = 0
    }
}