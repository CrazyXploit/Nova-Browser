-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.nova.browser.util.** { *; }
-dontwarn org.chromium.**