package com.nova.browser.data

/**
 * Injects Eruda DevTools into every page.
 *
 * Loaded from LOCAL ASSETS (file:///android_asset/eruda.js) — bypasses CSP.
 * Works on Google, Facebook, Twitter, and any other strict-CSP site.
 */
object ErudaInjector {

    var enabled: Boolean = true

    // Auto-inject at document-start. Loads Eruda from local assets.
    fun buildInitScript(): String = """
        (function() {
            if (window.__novaErudaLoaded) return;
            window.__novaErudaLoaded = true;

            if (location.href.startsWith('about:') ||
                location.href.startsWith('data:') ||
                location.href.includes('eruda')) {
                return;
            }

            function runEruda() {
                try {
                    if (typeof eruda === 'undefined') return;
                    eruda.init({
                        defaults: {
                            displaySize: 40,
                            transparency: 0.9,
                            theme: 'Dark'
                        },
                        tool: ['console', 'elements', 'network', 'resources', 'info']
                    });
                    eruda.show();
                } catch (e) {
                    console.log('Eruda init failed', e);
                }
            }

            var script = document.createElement('script');
            script.src = 'file:///android_asset/eruda.js';
            script.onload = runEruda;
            script.onerror = function() {
                console.log('Eruda local asset failed to load');
            };
            (document.head || document.documentElement).appendChild(script);
        })();
    """.trimIndent()
}