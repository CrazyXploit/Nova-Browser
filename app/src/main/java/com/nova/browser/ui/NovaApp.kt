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
import com.nova.browser.ui.downloads.DownloadsScreen
import com.nova.browser.ui.history.HistoryScreen

@Composable
fun NovaApp() {
    val nav = rememberNavController()

    NavHost(
        navController = nav,
        startDestination = "browser",
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(250),
            ) + fadeIn(tween(250))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 4 },
                animationSpec = tween(250),
            ) + fadeOut(tween(250))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 4 },
                animationSpec = tween(250),
            ) + fadeIn(tween(250))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(250),
            ) + fadeOut(tween(250))
        },
    ) {
        composable("browser") {
            BrowserScreen(
                onOpenBookmarks = { nav.navigate("bookmarks") },
                onOpenDownloads = { nav.navigate("downloads") },
                onOpenHistory = { nav.navigate("history") },
            )
        }
        composable("bookmarks") {
            BookmarksScreen(
                onBack = { nav.popBackStack() },
                onOpenUrl = { },
            )
        }
        composable("downloads") {
            DownloadsScreen(
                onBack = { nav.popBackStack() },
            )
        }
        composable("history") {
            HistoryScreen(
                onBack = { nav.popBackStack() },
                onOpenUrl = { url -> nav.navigate("browser") },
            )
        }
    }
}