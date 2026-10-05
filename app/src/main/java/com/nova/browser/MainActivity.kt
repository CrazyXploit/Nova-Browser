package com.nova.browser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.nova.browser.data.SearchEngineManager
import com.nova.browser.data.UsageStats
import com.nova.browser.data.UserAgentManager
import com.nova.browser.ui.NovaApp
import com.nova.browser.ui.theme.NovaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        UserAgentManager.load(this)
        SearchEngineManager.load(this)
        UsageStats.load(this)

        setContent {
            NovaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    NovaApp()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        UsageStats.save(this)
    }
}