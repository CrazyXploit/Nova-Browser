package com.nova.browser.util;

import androidx.annotation.NonNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

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

        String q;
        try {
            // Use the String-charset overload — works on ALL Android versions
            q = URLEncoder.encode(trimmed, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            q = trimmed;
        }
        return "https://duckduckgo.com/?q=" + q;
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