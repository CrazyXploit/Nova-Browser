package com.nova.browser.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches public IP from multiple providers. Tries each until one works.
 */
object MyIpFetcher {

    private const val TAG = "MyIpFetcher"

    private val providers = listOf(
        "https://api.ipify.org",
        "https://ipv4.icanhazip.com",
        "https://checkip.amazonaws.com",
        "https://ifconfig.me/ip",
        "https://api.my-ip.io/ip",
    )

    suspend fun fetch(): String = withContext(Dispatchers.IO) {
        for (provider in providers) {
            try {
                Log.d(TAG, "Trying $provider")
                val conn = (URL(provider).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 NovaBrowser")
                }
                val code = conn.responseCode
                if (code in 200..299) {
                    val ip = conn.inputStream.bufferedReader().use { it.readText().trim() }
                    conn.disconnect()
                    if (ip.isNotBlank() && ip.length <= 45 && !ip.contains(" ")) {
                        Log.d(TAG, "Got IP from $provider: $ip")
                        return@withContext ip
                    }
                } else {
                    Log.w(TAG, "$provider → HTTP $code")
                    conn.disconnect()
                }
            } catch (t: Throwable) {
                Log.w(TAG, "$provider failed: ${t.message}")
            }
        }
        "Unable to fetch IP"
    }
}