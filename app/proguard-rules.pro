# YTDow ProGuard rules
-keep class com.hermes.downloader.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
