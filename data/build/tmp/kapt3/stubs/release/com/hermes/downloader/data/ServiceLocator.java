package com.hermes.downloader.data;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010$\u001a\u00020%2\u0006\u0010\u0003\u001a\u00020\u0004R\u001a\u0010\u0003\u001a\u00020\u0004X\u0086.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\bR\u001b\u0010\t\u001a\u00020\n8FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\r\u0010\u000e\u001a\u0004\b\u000b\u0010\fR\u001b\u0010\u000f\u001a\u00020\u00108FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0013\u0010\u000e\u001a\u0004\b\u0011\u0010\u0012R\u001a\u0010\u0014\u001a\u00020\u0015X\u0086.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0016\u0010\u0017\"\u0004\b\u0018\u0010\u0019R\u001b\u0010\u001a\u001a\u00020\u001b8FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\u001e\u0010\u000e\u001a\u0004\b\u001c\u0010\u001dR\u001b\u0010\u001f\u001a\u00020 8FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b#\u0010\u000e\u001a\u0004\b!\u0010\"\u00a8\u0006&"}, d2 = {"Lcom/hermes/downloader/data/ServiceLocator;", "", "()V", "app", "Landroid/app/Application;", "getApp", "()Landroid/app/Application;", "setApp", "(Landroid/app/Application;)V", "downloadRepo", "Lcom/hermes/downloader/domain/repository/DownloadRepository;", "getDownloadRepo", "()Lcom/hermes/downloader/domain/repository/DownloadRepository;", "downloadRepo$delegate", "Lkotlin/Lazy;", "logger", "Lcom/hermes/downloader/core/Logger;", "getLogger", "()Lcom/hermes/downloader/core/Logger;", "logger$delegate", "prefs", "Landroid/content/SharedPreferences;", "getPrefs", "()Landroid/content/SharedPreferences;", "setPrefs", "(Landroid/content/SharedPreferences;)V", "queueManager", "Lcom/hermes/downloader/domain/queue/QueueManager;", "getQueueManager", "()Lcom/hermes/downloader/domain/queue/QueueManager;", "queueManager$delegate", "settingsRepo", "Lcom/hermes/downloader/domain/repository/SettingsRepository;", "getSettingsRepo", "()Lcom/hermes/downloader/domain/repository/SettingsRepository;", "settingsRepo$delegate", "init", "", "data_release"})
public final class ServiceLocator {
    public static android.app.Application app;
    public static android.content.SharedPreferences prefs;
    @org.jetbrains.annotations.NotNull
    private static final kotlin.Lazy downloadRepo$delegate = null;
    @org.jetbrains.annotations.NotNull
    private static final kotlin.Lazy settingsRepo$delegate = null;
    @org.jetbrains.annotations.NotNull
    private static final kotlin.Lazy queueManager$delegate = null;
    @org.jetbrains.annotations.NotNull
    private static final kotlin.Lazy logger$delegate = null;
    @org.jetbrains.annotations.NotNull
    public static final com.hermes.downloader.data.ServiceLocator INSTANCE = null;
    
    private ServiceLocator() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final android.app.Application getApp() {
        return null;
    }
    
    public final void setApp(@org.jetbrains.annotations.NotNull
    android.app.Application p0) {
    }
    
    @org.jetbrains.annotations.NotNull
    public final android.content.SharedPreferences getPrefs() {
        return null;
    }
    
    public final void setPrefs(@org.jetbrains.annotations.NotNull
    android.content.SharedPreferences p0) {
    }
    
    @org.jetbrains.annotations.NotNull
    public final com.hermes.downloader.domain.repository.DownloadRepository getDownloadRepo() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final com.hermes.downloader.domain.repository.SettingsRepository getSettingsRepo() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final com.hermes.downloader.domain.queue.QueueManager getQueueManager() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final com.hermes.downloader.core.Logger getLogger() {
        return null;
    }
    
    public final void init(@org.jetbrains.annotations.NotNull
    android.app.Application app) {
    }
}