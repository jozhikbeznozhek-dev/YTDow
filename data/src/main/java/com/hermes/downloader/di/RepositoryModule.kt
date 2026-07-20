package com.hermes.downloader.di

import com.hermes.downloader.data.repository.UpdateRepositoryImpl
import com.hermes.downloader.domain.repository.UpdateRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides @Singleton
    fun provideUpdateRepository(): UpdateRepository = UpdateRepositoryImpl("2.1.0")
}
