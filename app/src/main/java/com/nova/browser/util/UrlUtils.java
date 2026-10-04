package com.nova.browser.util;

import androidx.annotation.NonNull;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Java utility for URL normalization — interop demo with Kotlin code.
 * Called from BrowserViewModel via UrlUtils.normalize(input).
 */
public final class UrlUtils {

    private UrlUtils() {}

    @NonNull
    public static String normalize(@NonNull String input) {
        String trimmed = input.trim();
        if (trimmed.isEmpty()) return "about:blank";

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        if (trimmed.contains(".") && !trimmed.contains(" ")) {
            return "https://" + trimmed;
        }
        String q = URLEncoder.encode(trimmed, StandardCharsets.UTF_8);
        return "https://duckduckgo.com/?q=" + q;
    }

    public static boolean isSecure(@NonNull String url) {
        return url.startsWith("https://");
    }

    public static String displayHost(@NonNull String url) {
        try {
            java.net.URL u = new java.net.URL(url);
            String host = u.getHost();
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception e) {
            return url;
        }
    }
}