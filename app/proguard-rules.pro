# YTDow ProGuard rules
-keep class com.hermes.downloader.** { *; }
-keep class com.yausername.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# youtubedl-android uses Commons Compress for ZIP extraction only. Commons Compress
# references these optional XZ/Zstandard codecs, but YTDow never invokes those paths.
-dontwarn com.github.luben.zstd.ZstdInputStream
-dontwarn org.tukaani.xz.MemoryLimitException
-dontwarn org.tukaani.xz.SingleXZInputStream
-dontwarn org.tukaani.xz.XZInputStream
