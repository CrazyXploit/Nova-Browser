# Keep Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper

# Keep WebView JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep WebViewAssetLoader
-keep class androidx.webkit.** { *; }

# Keep our model classes
-keep class com.nova.browser.data.** { *; }
-keep class com.nova.browser.util.** { *; }

# Keep Coil
-dontwarn coil.**

# Kotlin metadata
-keep class kotlin.Metadata { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**