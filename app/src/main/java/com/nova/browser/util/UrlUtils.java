package com.nova.browser.util;

import androidx.annotation.NonNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * URL normalization. Uses SearchEngineManager for search queries.
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

        // Delegate search to the manager (handles current engine)
        return com.nova.browser.data.SearchEngineManager.INSTANCE.buildSearchUrl(trimmed);
    }

    public static boolean isSecure(@NonNull String url) {
        return url.startsWith("https://");
    }

    public static String displayHost(@NonNull String url) {
        try {
            java.net.URL u = new java.net.URL(url);
            String host = u.getHost();
            if (host == null) return url;
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception e) {
            return url;
        }
    }
}