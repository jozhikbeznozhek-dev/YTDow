package com.hermes.downloader.di

import com.hermes.downloader.data.repository.DownloadRepositoryImpl
import com.hermes.downloader.data.repository.SettingsRepositoryImpl
import com.hermes.downloader.domain.repository.DownloadRepository
import com.hermes.downloader.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindsModule {
    @Binds @Singleton abstract fun bindDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository
    @Binds @Singleton abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
