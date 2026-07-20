package com.hermes.downloader.di

import android.content.Context
import android.content.SharedPreferences
import com.hermes.downloader.core.Logger
import com.hermes.downloader.data.AndroidLogger
import com.hermes.downloader.domain.queue.QueueManager
import com.hermes.downloader.data.queue.QueueManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideSharedPreferences(@ApplicationContext ctx: Context): SharedPreferences =
        ctx.getSharedPreferences("ytdow", Context.MODE_PRIVATE)

    @Provides @Singleton
    fun provideLogger(): Logger = AndroidLogger()

    @Provides @Singleton
    fun provideQueueManager(logger: Logger): QueueManager = QueueManagerImpl(logger)
}
