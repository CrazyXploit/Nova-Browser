package com.nova.browser.data

/**
 * Reader mode — extracts article content and displays it cleanly.
 * Uses a simplified Readability approach in JavaScript.
 */
object ReaderModeInjector {

    @Volatile var active: Boolean = false

    fun buildScript(): String = """
        (function() {
            if (window.__novaReaderMode) return;
            window.__novaReaderMode = true;

            // Extract main content heuristically
            function getMainContent() {
                var candidates = [
                    'article',
                    '[role="main"]',
                    'main',
                    '.post-content',
                    '.article-content',
                    '.entry-content',
                    '.content',
                    '#content',
                    '.post',
                    '.article'
                ];
                for (var i = 0; i < candidates.length; i++) {
                    var el = document.querySelector(candidates[i]);
                    if (el && el.innerText && el.innerText.length > 500) {
                        return el;
                    }
                }
                // Fallback: find the div with most paragraphs
                var best = null;
                var bestScore = 0;
                document.querySelectorAll('div, section, article').forEach(function(el) {
                    var pCount = el.querySelectorAll('p').length;
                    if (pCount > bestScore) {
                        bestScore = pCount;
                        best = el;
                    }
                });
                return best;
            }

            var content = getMainContent();
            if (!content) {
                alert('Could not extract article content on this page.');
                window.__novaReaderMode = false;
                return;
            }

            var title = document.title || '';
            var h1 = document.querySelector('h1');
            var heading = h1 ? h1.innerText : title;
            var byline = '';
            var author = document.querySelector('[rel="author"], .author, .byline');
            if (author) byline = author.innerText;

            var html = content.innerHTML;

            // Reader CSS
            var css = `
                html, body {
                    background: #0A0A0A !important;
                    color: #ECECEC !important;
                    margin: 0 !important;
                    padding: 0 !important;
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif !important;
                    font-size: 18px !important;
                    line-height: 1.7 !important;
                }
                #nova-reader-root {
                    max-width: 700px;
                    margin: 0 auto;
                    padding: 24px 20px 60px 20px;
                }
                #nova-reader-title {
                    font-size: 28px;
                    font-weight: 700;
                    line-height: 1.3;
                    margin: 0 0 12px 0;
                    color: #FFFFFF;
                }
                #nova-reader-byline {
                    font-size: 14px;
                    color: #9A9A9A;
                    margin-bottom: 32px;
                }
                #nova-reader-content img {
                    max-width: 100%;
                    height: auto;
                    border-radius: 8px;
                    margin: 16px 0;
                }
                #nova-reader-content a {
                    color: #7C5CFF;
                    text-decoration: none;
                }
                #nova-reader-content p {
                    margin: 0 0 20px 0;
                }
                #nova-reader-content h1, #nova-reader-content h2, #nova-reader-content h3 {
                    color: #FFFFFF;
                    margin-top: 32px;
                    margin-bottom: 16px;
                }
                #nova-reader-content blockquote {
                    border-left: 3px solid #7C5CFF;
                    padding-left: 16px;
                    margin: 20px 0;
                    color: #B0B0B0;
                    font-style: italic;
                }
                #nova-reader-content pre, #nova-reader-content code {
                    background: #1A1A1A;
                    padding: 2px 6px;
                    border-radius: 4px;
                    font-family: monospace;
                    font-size: 15px;
                }
                #nova-reader-content pre {
                    padding: 16px;
                    overflow-x: auto;
                }
                #nova-reader-exit {
                    position: fixed;
                    top: 16px;
                    right: 16px;
                    z-index: 999999;
                    background: #7C5CFF;
                    color: white;
                    border: none;
                    border-radius: 20px;
                    padding: 8px 16px;
                    font-size: 14px;
                    font-weight: 600;
                    cursor: pointer;
                    box-shadow: 0 4px 12px rgba(0,0,0,0.4);
                }
            `;

            // Build clean page
            document.documentElement.innerHTML = `
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>${'$'}{title} — Reader Mode</title>
                    <style>${'$'}{css}</style>
                </head>
                <body>
                    <button id="nova-reader-exit" onclick="window.location.reload()">Exit Reader</button>
                    <div id="nova-reader-root">
                        <h1 id="nova-reader-title">${'$'}{heading}</h1>
                        ${'$'}{byline ? '<div id="nova-reader-byline">' + byline + '</div>' : ''}
                        <div id="nova-reader-content">${'$'}{html}</div>
                    </div>
                </body>
            `;

            console.log('Nova: Reader mode active');
        })();
    """.trimIndent()
}