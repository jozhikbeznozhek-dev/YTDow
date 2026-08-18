# YTDow ProGuard rules
-keep class com.hermes.downloader.** { *; }
-keep class com.yausername.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Optional Commons Compress codecs are not used by YTDow's yt-dlp update path.
-dontwarn com.github.luben.zstd.ZstdInputStream
-dontwarn org.tukaani.xz.MemoryLimitException
-dontwarn org.tukaani.xz.SingleXZInputStream
-dontwarn org.tukaani.xz.XZInputStream
