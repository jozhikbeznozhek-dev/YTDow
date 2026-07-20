package com.hermes.downloader.data.local;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\'\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H&J\b\u0010\u0005\u001a\u00020\u0006H&J\b\u0010\u0007\u001a\u00020\bH&\u00a8\u0006\t"}, d2 = {"Lcom/hermes/downloader/data/local/YTDowDatabase;", "Landroidx/room/RoomDatabase;", "()V", "historyDao", "Lcom/hermes/downloader/data/local/HistoryDao;", "settingsDao", "Lcom/hermes/downloader/data/local/SettingsDao;", "taskDao", "Lcom/hermes/downloader/data/local/TaskDao;", "data_debug"})
@androidx.room.Database(entities = {com.hermes.downloader.data.local.HistoryEntity.class, com.hermes.downloader.data.local.TaskEntity.class, com.hermes.downloader.data.local.SettingsEntity.class}, version = 1, exportSchema = false)
public abstract class YTDowDatabase extends androidx.room.RoomDatabase {
    
    public YTDowDatabase() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public abstract com.hermes.downloader.data.local.HistoryDao historyDao();
    
    @org.jetbrains.annotations.NotNull
    public abstract com.hermes.downloader.data.local.TaskDao taskDao();
    
    @org.jetbrains.annotations.NotNull
    public abstract com.hermes.downloader.data.local.SettingsDao settingsDao();
}