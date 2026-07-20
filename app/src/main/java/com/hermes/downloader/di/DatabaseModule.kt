package com.hermes.downloader.di

import android.content.Context
import androidx.room.Room
import com.hermes.downloader.data.local.YTDowDatabase
import com.hermes.downloader.data.local.HistoryDao
import com.hermes.downloader.data.local.TaskDao
import com.hermes.downloader.data.local.SettingsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): YTDowDatabase =
        Room.databaseBuilder(ctx, YTDowDatabase::class.java, "ytdow.db").build()

    @Provides fun provideHistoryDao(db: YTDowDatabase): HistoryDao = db.historyDao()
    @Provides fun provideTaskDao(db: YTDowDatabase): TaskDao = db.taskDao()
    @Provides fun provideSettingsDao(db: YTDowDatabase): SettingsDao = db.settingsDao()
}
