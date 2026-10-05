package com.nova.browser.data

/**
 * Injects Eruda DevTools into every page.
 * Eruda is a mobile browser console with Elements, Console, Network, Resources tabs.
 *
 * Loaded from CDN on first use, then cached by the WebView.
 * Set `ErudaInjector.enabled = false` to disable.
 */
object ErudaInjector {

    var enabled: Boolean = true

    // Eruda CDN (lightweight, ~150KB)
    private const val ERUDA_CDN = "https://cdn.jsdelivr.net/npm/eruda"

    // Auto-init script: loads Eruda, initializes it, and shows the floating button.
    // Runs at document-start (before page JS) via evaluateJavascript.
    fun buildInitScript(): String = """
        (function() {
            if (window.__novaErudaLoaded) return;
            window.__novaErudaLoaded = true;

            // Don't inject into Eruda's own popup or blank pages
            if (location.href.startsWith('about:') ||
                location.href.startsWith('data:') ||
                location.href.includes('eruda')) {
                return;
            }

            var script = document.createElement('script');
            script.src = '$ERUDA_CDN';
            script.onload = function() {
                try {
                    eruda.init({
                        defaults: {
                            displaySize: 50,
                            transparency: 0.9,
                            theme: 'Dark'
                        },
                        tool: ['console', 'elements', 'network', 'resources', 'info']
                    });
                    // Auto-open the console panel
                    eruda.show();
                } catch (e) {
                    console.log('Eruda init failed', e);
                }
            };
            script.onerror = function() {
                console.log('Eruda CDN failed to load');
            };
            (document.head || document.documentElement).appendChild(script);
        })();
    """.trimIndent()
}