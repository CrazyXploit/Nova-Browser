package com.nova.browser.data

import android.content.Context
import android.content.Intent

/**
 * Quick Search — a shortcut tile / widget that opens the browser ready to search.
 * Triggered from a launcher shortcut or notification.
 */
object QuickSearchService {

    const val ACTION_QUICK_SEARCH = "com.nova.browser.QUICK_SEARCH"

    fun buildIntent(context: Context, packageName: String): Intent {
        return Intent(Intent.ACTION_MAIN).apply {
            setClassName(packageName, "${packageName}.QuickSearchActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("quick_search", true)
        }
    }
}