package com.hermes.downloader.data.local;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0004\bg\u0018\u00002\u00020\u0001J\u000e\u0010\u0002\u001a\u00020\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u0004J\u0016\u0010\u0005\u001a\u00020\u00032\u0006\u0010\u0006\u001a\u00020\u0007H\u00a7@\u00a2\u0006\u0002\u0010\bJ\u0014\u0010\t\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\u000b0\nH\'J\u0016\u0010\r\u001a\u00020\u00032\u0006\u0010\u000e\u001a\u00020\fH\u00a7@\u00a2\u0006\u0002\u0010\u000f\u00a8\u0006\u0010"}, d2 = {"Lcom/hermes/downloader/data/local/HistoryDao;", "", "clearAll", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteByFilePath", "filePath", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAll", "Lkotlinx/coroutines/flow/Flow;", "", "Lcom/hermes/downloader/data/local/HistoryEntity;", "insert", "entry", "(Lcom/hermes/downloader/data/local/HistoryEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "data_debug"})
@androidx.room.Dao
public abstract interface HistoryDao {
    
    @androidx.room.Query(value = "SELECT * FROM download_history ORDER BY created_at DESC LIMIT 200")
    @org.jetbrains.annotations.NotNull
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.hermes.downloader.data.local.HistoryEntity>> getAll();
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object insert(@org.jetbrains.annotations.NotNull
    com.hermes.downloader.data.local.HistoryEntity entry, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "DELETE FROM download_history WHERE file_path = :filePath")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object deleteByFilePath(@org.jetbrains.annotations.NotNull
    java.lang.String filePath, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "DELETE FROM download_history")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object clearAll(@org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}