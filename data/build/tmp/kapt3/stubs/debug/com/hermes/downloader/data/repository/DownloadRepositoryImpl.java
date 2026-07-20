package com.hermes.downloader.data.repository;

@javax.inject.Singleton
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000z\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u000b\n\u0002\u0010\u000b\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\b\b\u0007\u0018\u00002\u00020\u0001B\u0019\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0016\u0010\u001e\u001a\u00020\u001f2\u0006\u0010 \u001a\u00020\u000eH\u0096@\u00a2\u0006\u0002\u0010!J\u0010\u0010\"\u001a\u00020\u001f2\u0006\u0010#\u001a\u00020\nH\u0016J\u000e\u0010$\u001a\u00020\u001fH\u0096@\u00a2\u0006\u0002\u0010%J\u0012\u0010&\u001a\u0004\u0018\u00010\n2\u0006\u0010\'\u001a\u00020\nH\u0002J\u0010\u0010(\u001a\n )*\u0004\u0018\u00010\n0\nH\u0002J\u0016\u0010*\u001a\u00020+2\u0006\u0010,\u001a\u00020\nH\u0096@\u00a2\u0006\u0002\u0010-J\u0006\u0010.\u001a\u00020\u001fJ\u0016\u0010/\u001a\u00020\n2\u0006\u00100\u001a\u00020\u000bH\u0096@\u00a2\u0006\u0002\u00101J\u0010\u00102\u001a\u0004\u0018\u0001032\u0006\u0010,\u001a\u00020\nJ\u0014\u00104\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000e0\r0\u0015H\u0016J.\u00105\u001a\u0002062\u0006\u00107\u001a\u00020\n2\u0006\u00108\u001a\u00020\n2\u0006\u00109\u001a\u00020\n2\u0006\u0010:\u001a\u00020\nH\u0096@\u00a2\u0006\u0002\u0010;J\u000e\u0010<\u001a\b\u0012\u0004\u0012\u00020\u000e0\rH\u0002J\u0016\u0010=\u001a\u00020\u001f2\u0006\u0010,\u001a\u00020\nH\u0096@\u00a2\u0006\u0002\u0010-R \u0010\u0007\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\u000b0\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\f\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000e0\r0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u000f\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00100\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0011\u001a\u000e\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\u00130\u0012X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R#\u0010\u0014\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\u000b0\t0\u0015\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0017R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0018\u001a\u00020\u0019X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001a\u001a\u00020\u001bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u001c\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00100\u0015\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u0017\u00a8\u0006>"}, d2 = {"Lcom/hermes/downloader/data/repository/DownloadRepositoryImpl;", "Lcom/hermes/downloader/domain/repository/DownloadRepository;", "context", "Landroid/content/Context;", "logger", "Lcom/hermes/downloader/core/Logger;", "(Landroid/content/Context;Lcom/hermes/downloader/core/Logger;)V", "_currentTasks", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "", "Lcom/hermes/downloader/domain/model/DownloadTask;", "_history", "", "Lcom/hermes/downloader/domain/model/DownloadHistoryEntry;", "_taskEvents", "Lcom/hermes/downloader/data/repository/TaskEvent;", "active", "Ljava/util/concurrent/ConcurrentHashMap;", "Lkotlinx/coroutines/Job;", "currentTasks", "Lkotlinx/coroutines/flow/Flow;", "getCurrentTasks", "()Lkotlinx/coroutines/flow/Flow;", "prefs", "Landroid/content/SharedPreferences;", "scope", "Lkotlinx/coroutines/CoroutineScope;", "taskEvents", "getTaskEvents", "addToHistory", "", "entry", "(Lcom/hermes/downloader/domain/model/DownloadHistoryEntry;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "cancelDownload", "taskId", "clearHistory", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "copyToPublic", "srcPath", "defaultPath", "kotlin.jvm.PlatformType", "deleteFile", "", "filePath", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "destroy", "executeDownload", "task", "(Lcom/hermes/downloader/domain/model/DownloadTask;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "findDownloadUri", "Landroid/net/Uri;", "getHistory", "getVideoMetadata", "Lcom/hermes/downloader/domain/model/VideoMetadata;", "url", "format", "quality", "audioLang", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "loadHistory", "removeFromHistory", "data_debug"})
public final class DownloadRepositoryImpl implements com.hermes.downloader.domain.repository.DownloadRepository {
    @org.jetbrains.annotations.NotNull
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull
    private final com.hermes.downloader.core.Logger logger = null;
    @org.jetbrains.annotations.NotNull
    private final android.content.SharedPreferences prefs = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.CoroutineScope scope = null;
    @org.jetbrains.annotations.NotNull
    private final java.util.concurrent.ConcurrentHashMap<java.lang.String, kotlinx.coroutines.Job> active = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.Map<java.lang.String, com.hermes.downloader.domain.model.DownloadTask>> _currentTasks = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.Flow<java.util.Map<java.lang.String, com.hermes.downloader.domain.model.DownloadTask>> currentTasks = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.MutableStateFlow<com.hermes.downloader.data.repository.TaskEvent> _taskEvents = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.Flow<com.hermes.downloader.data.repository.TaskEvent> taskEvents = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.hermes.downloader.domain.model.DownloadHistoryEntry>> _history = null;
    
    @javax.inject.Inject
    public DownloadRepositoryImpl(@dagger.hilt.android.qualifiers.ApplicationContext
    @org.jetbrains.annotations.NotNull
    android.content.Context context, @org.jetbrains.annotations.NotNull
    com.hermes.downloader.core.Logger logger) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.Flow<java.util.Map<java.lang.String, com.hermes.downloader.domain.model.DownloadTask>> getCurrentTasks() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.Flow<com.hermes.downloader.data.repository.TaskEvent> getTaskEvents() {
        return null;
    }
    
    public final void destroy() {
    }
    
    @java.lang.Override
    @org.jetbrains.annotations.NotNull
    public kotlinx.coroutines.flow.Flow<java.util.List<com.hermes.downloader.domain.model.DownloadHistoryEntry>> getHistory() {
        return null;
    }
    
    @java.lang.Override
    @org.jetbrains.annotations.Nullable
    public java.lang.Object addToHistory(@org.jetbrains.annotations.NotNull
    com.hermes.downloader.domain.model.DownloadHistoryEntry entry, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override
    @org.jetbrains.annotations.Nullable
    public java.lang.Object removeFromHistory(@org.jetbrains.annotations.NotNull
    java.lang.String filePath, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override
    @org.jetbrains.annotations.Nullable
    public java.lang.Object clearHistory(@org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override
    @org.jetbrains.annotations.Nullable
    public java.lang.Object getVideoMetadata(@org.jetbrains.annotations.NotNull
    java.lang.String url, @org.jetbrains.annotations.NotNull
    java.lang.String format, @org.jetbrains.annotations.NotNull
    java.lang.String quality, @org.jetbrains.annotations.NotNull
    java.lang.String audioLang, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.hermes.downloader.domain.model.VideoMetadata> $completion) {
        return null;
    }
    
    @java.lang.Override
    @org.jetbrains.annotations.Nullable
    public java.lang.Object executeDownload(@org.jetbrains.annotations.NotNull
    com.hermes.downloader.domain.model.DownloadTask task, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    @java.lang.Override
    public void cancelDownload(@org.jetbrains.annotations.NotNull
    java.lang.String taskId) {
    }
    
    @java.lang.Override
    @org.jetbrains.annotations.Nullable
    public java.lang.Object deleteFile(@org.jetbrains.annotations.NotNull
    java.lang.String filePath, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final android.net.Uri findDownloadUri(@org.jetbrains.annotations.NotNull
    java.lang.String filePath) {
        return null;
    }
    
    private final java.lang.String copyToPublic(java.lang.String srcPath) {
        return null;
    }
    
    private final java.util.List<com.hermes.downloader.domain.model.DownloadHistoryEntry> loadHistory() {
        return null;
    }
    
    private final java.lang.String defaultPath() {
        return null;
    }
}