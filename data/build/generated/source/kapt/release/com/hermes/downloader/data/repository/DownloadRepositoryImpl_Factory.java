package com.hermes.downloader.data.repository;

import android.content.Context;
import com.hermes.downloader.core.Logger;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class DownloadRepositoryImpl_Factory implements Factory<DownloadRepositoryImpl> {
  private final Provider<Context> contextProvider;

  private final Provider<Logger> loggerProvider;

  public DownloadRepositoryImpl_Factory(Provider<Context> contextProvider,
      Provider<Logger> loggerProvider) {
    this.contextProvider = contextProvider;
    this.loggerProvider = loggerProvider;
  }

  @Override
  public DownloadRepositoryImpl get() {
    return newInstance(contextProvider.get(), loggerProvider.get());
  }

  public static DownloadRepositoryImpl_Factory create(Provider<Context> contextProvider,
      Provider<Logger> loggerProvider) {
    return new DownloadRepositoryImpl_Factory(contextProvider, loggerProvider);
  }

  public static DownloadRepositoryImpl newInstance(Context context, Logger logger) {
    return new DownloadRepositoryImpl(context, logger);
  }
}
