package com.nova.browser.data

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

object AdBlocker {

    private val blockedHosts = setOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "google-analytics.com",
        "googletagmanager.com",
        "googletagservices.com",
        "adservice.google.com",
        "ads.google.com",
        "pagead2.googlesyndication.com",
        "partner.googleadservices.com",
        "adnxs.com",
        "ads.yahoo.com",
        "advertising.com",
        "amazon-adsystem.com",
        "facebook.net",
        "fbcdn.net",
        "scorecardresearch.com",
        "outbrain.com",
        "taboola.com",
        "criteo.com",
        "criteo.net",
        "pubmatic.com",
        "rubiconproject.com",
        "openx.net",
        "casalemedia.com",
        "smartadserver.com",
        "sharethrough.com",
        "teads.tv",
        "adform.net",
        "bidswitch.net",
        "adroll.com",
        "quantserve.com",
        "hotjar.com",
        "mixpanel.com",
        "segment.io",
        "segment.com",
        "amplitude.com",
        "branch.io",
        "appsflyer.com",
        "adjust.com",
    )

    private val blockedKeywords = setOf(
        "/ads/",
        "/ad/",
        "/adserver/",
        "/advert/",
        "/banner/",
        "/popup/",
        "/tracking/",
        "/tracker/",
        "/analytics/",
        "/pixel/",
        "/beacon/",
    )

    private val emptyResponse: WebResourceResponse
        get() = WebResourceResponse(
            "text/plain",
            "utf-8",
            ByteArrayInputStream(ByteArray(0)),
        )

    var enabled: Boolean = true

    fun shouldBlock(url: String?): Boolean {
        if (!enabled || url.isNullOrEmpty()) return false
        val lower = url.lowercase()

        for (host in blockedHosts) {
            if (lower.contains(host)) return true
        }
        for (kw in blockedKeywords) {
            if (lower.contains(kw)) return true
        }
        return false
    }

    fun blockedResponse(): WebResourceResponse = emptyResponse
}