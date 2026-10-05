package com.nova.browser.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nova.browser.ui.bookmarks.BookmarksScreen
import com.nova.browser.ui.browser.BrowserScreen
import com.nova.browser.ui.browser.BrowserViewModel
import com.nova.browser.ui.downloads.DownloadsScreen

@Composable
fun NovaApp() {
    val nav = rememberNavController()

    NavHost(
        navController = nav,
        startDestination = "browser",
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300),
            ) + fadeIn(tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 4 },
                animationSpec = tween(300),
            ) + fadeOut(tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 4 },
                animationSpec = tween(300),
            ) + fadeIn(tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300),
            ) + fadeOut(tween(300))
        },
    ) {
        composable("browser") {
            BrowserScreen(
                onOpenBookmarks = { nav.navigate("bookmarks") },
                onOpenDownloads = { nav.navigate("downloads") },
            )
        }
        composable("bookmarks") {
            BookmarksScreen(
                onBack = { nav.popBackStack() },
                onOpenUrl = { url ->
                    // Not needed — the browser opens on next composition
                },
            )
        }
        composable("downloads") {
            DownloadsScreen(
                onBack = { nav.popBackStack() },
            )
        }
    }
}