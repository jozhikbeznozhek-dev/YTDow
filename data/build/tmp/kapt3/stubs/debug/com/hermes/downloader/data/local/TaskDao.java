package com.hermes.downloader.data.local;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\b\n\u0002\b\u0005\bg\u0018\u00002\u00020\u0001J\u000e\u0010\u0002\u001a\u00020\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u0004J\u0016\u0010\u0005\u001a\u00020\u00032\u0006\u0010\u0006\u001a\u00020\u0007H\u00a7@\u00a2\u0006\u0002\u0010\bJ\u0014\u0010\t\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\u000b0\nH\'J\u0016\u0010\r\u001a\u00020\u00032\u0006\u0010\u000e\u001a\u00020\fH\u00a7@\u00a2\u0006\u0002\u0010\u000fJ\u001e\u0010\u0010\u001a\u00020\u00032\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\u0011\u001a\u00020\u0012H\u00a7@\u00a2\u0006\u0002\u0010\u0013J\u001e\u0010\u0014\u001a\u00020\u00032\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\u0015\u001a\u00020\u0007H\u00a7@\u00a2\u0006\u0002\u0010\u0016\u00a8\u0006\u0017"}, d2 = {"Lcom/hermes/downloader/data/local/TaskDao;", "", "clearAll", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "delete", "taskId", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAll", "Lkotlinx/coroutines/flow/Flow;", "", "Lcom/hermes/downloader/data/local/TaskEntity;", "insert", "task", "(Lcom/hermes/downloader/data/local/TaskEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateProgress", "progress", "", "(Ljava/lang/String;ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateState", "state", "(Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "data_debug"})
@androidx.room.Dao
public abstract interface TaskDao {
    
    @androidx.room.Query(value = "SELECT * FROM tasks ORDER BY priority DESC, created_at ASC")
    @org.jetbrains.annotations.NotNull
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.hermes.downloader.data.local.TaskEntity>> getAll();
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object insert(@org.jetbrains.annotations.NotNull
    com.hermes.downloader.data.local.TaskEntity task, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "UPDATE tasks SET state = :state WHERE id = :taskId")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object updateState(@org.jetbrains.annotations.NotNull
    java.lang.String taskId, @org.jetbrains.annotations.NotNull
    java.lang.String state, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "UPDATE tasks SET progress = :progress, state = \'DOWNLOADING\' WHERE id = :taskId")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object updateProgress(@org.jetbrains.annotations.NotNull
    java.lang.String taskId, int progress, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "DELETE FROM tasks WHERE id = :taskId")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object delete(@org.jetbrains.annotations.NotNull
    java.lang.String taskId, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "DELETE FROM tasks")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object clearAll(@org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}