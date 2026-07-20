package com.hermes.downloader.data.queue;

@javax.inject.Singleton
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000T\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\n\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010\u0013\u001a\u00020\nJ\u0010\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u0007H\u0016J\u0016\u0010\u0017\u001a\u00020\u00152\f\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00110\u0019H\u0016J\u000e\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00110\u0019H\u0016J\n\u0010\u001b\u001a\u0004\u0018\u00010\u0011H\u0016J \u0010\u001c\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u00072\u0006\u0010\u001d\u001a\u00020\u00072\u0006\u0010\u001e\u001a\u00020\u001fH\u0016J\u0018\u0010 \u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u00072\u0006\u0010!\u001a\u00020\u0007H\u0016J\u0018\u0010\"\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u00072\u0006\u0010#\u001a\u00020$H\u0016J(\u0010%\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u00072\u0006\u0010&\u001a\u00020\n2\u0006\u0010\'\u001a\u00020\u00072\u0006\u0010(\u001a\u00020\u0007H\u0016J\u0010\u0010)\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u0007H\u0016J\b\u0010*\u001a\u00020\u0015H\u0002J\u0006\u0010+\u001a\u00020\nJ\u0010\u0010,\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u0007H\u0016J\u0010\u0010-\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u0007H\u0016R\u001a\u0010\u0005\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\t\u001a\u00020\nX\u0096\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000b\u0010\f\"\u0004\b\r\u0010\u000eR\u0014\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00110\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0012\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00110\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006."}, d2 = {"Lcom/hermes/downloader/data/queue/QueueManagerImpl;", "Lcom/hermes/downloader/domain/queue/QueueManager;", "logger", "Lcom/hermes/downloader/core/Logger;", "(Lcom/hermes/downloader/core/Logger;)V", "downloading", "Ljava/util/concurrent/ConcurrentHashMap;", "", "", "maxConcurrent", "", "getMaxConcurrent", "()I", "setMaxConcurrent", "(I)V", "queue", "Ljava/util/concurrent/PriorityBlockingQueue;", "Lcom/hermes/downloader/domain/queue/QueueTask;", "tasks", "activeCount", "cancel", "", "taskId", "enqueue", "newTasks", "", "getAllTasks", "nextDownloadTask", "onTaskCompleted", "filePath", "historyEntry", "Lcom/hermes/downloader/domain/model/DownloadHistoryEntry;", "onTaskFailed", "error", "onTaskPrepared", "metadata", "Lcom/hermes/downloader/domain/model/VideoMetadata;", "onTaskProgress", "progress", "speed", "eta", "pause", "processQueue", "queuedCount", "resume", "retry", "data_release"})
public final class QueueManagerImpl implements com.hermes.downloader.domain.queue.QueueManager {
    @org.jetbrains.annotations.NotNull
    private final com.hermes.downloader.core.Logger logger = null;
    @org.jetbrains.annotations.NotNull
    private final java.util.concurrent.PriorityBlockingQueue<com.hermes.downloader.domain.queue.QueueTask> queue = null;
    @org.jetbrains.annotations.NotNull
    private final java.util.concurrent.ConcurrentHashMap<java.lang.String, com.hermes.downloader.domain.queue.QueueTask> tasks = null;
    @org.jetbrains.annotations.NotNull
    private final java.util.concurrent.ConcurrentHashMap<java.lang.String, java.lang.Boolean> downloading = null;
    private int maxConcurrent = 3;
    
    @javax.inject.Inject
    public QueueManagerImpl(@org.jetbrains.annotations.NotNull
    com.hermes.downloader.core.Logger logger) {
        super();
    }
    
    @java.lang.Override
    public int getMaxConcurrent() {
        return 0;
    }
    
    @java.lang.Override
    public void setMaxConcurrent(int p0) {
    }
    
    @java.lang.Override
    public void enqueue(@org.jetbrains.annotations.NotNull
    java.util.List<com.hermes.downloader.domain.queue.QueueTask> newTasks) {
    }
    
    @java.lang.Override
    public void cancel(@org.jetbrains.annotations.NotNull
    java.lang.String taskId) {
    }
    
    @java.lang.Override
    public void pause(@org.jetbrains.annotations.NotNull
    java.lang.String taskId) {
    }
    
    @java.lang.Override
    public void resume(@org.jetbrains.annotations.NotNull
    java.lang.String taskId) {
    }
    
    @java.lang.Override
    public void retry(@org.jetbrains.annotations.NotNull
    java.lang.String taskId) {
    }
    
    @java.lang.Override
    @org.jetbrains.annotations.NotNull
    public java.util.List<com.hermes.downloader.domain.queue.QueueTask> getAllTasks() {
        return null;
    }
    
    @java.lang.Override
    public void onTaskCompleted(@org.jetbrains.annotations.NotNull
    java.lang.String taskId, @org.jetbrains.annotations.NotNull
    java.lang.String filePath, @org.jetbrains.annotations.NotNull
    com.hermes.downloader.domain.model.DownloadHistoryEntry historyEntry) {
    }
    
    @java.lang.Override
    public void onTaskFailed(@org.jetbrains.annotations.NotNull
    java.lang.String taskId, @org.jetbrains.annotations.NotNull
    java.lang.String error) {
    }
    
    @java.lang.Override
    public void onTaskProgress(@org.jetbrains.annotations.NotNull
    java.lang.String taskId, int progress, @org.jetbrains.annotations.NotNull
    java.lang.String speed, @org.jetbrains.annotations.NotNull
    java.lang.String eta) {
    }
    
    @java.lang.Override
    public void onTaskPrepared(@org.jetbrains.annotations.NotNull
    java.lang.String taskId, @org.jetbrains.annotations.NotNull
    com.hermes.downloader.domain.model.VideoMetadata metadata) {
    }
    
    @java.lang.Override
    @org.jetbrains.annotations.Nullable
    public com.hermes.downloader.domain.queue.QueueTask nextDownloadTask() {
        return null;
    }
    
    private final void processQueue() {
    }
    
    public final int activeCount() {
        return 0;
    }
    
    public final int queuedCount() {
        return 0;
    }
}