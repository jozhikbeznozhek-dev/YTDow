package com.hermes.downloader.data.queue;

import com.hermes.downloader.core.Logger;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
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
public final class QueueManagerImpl_Factory implements Factory<QueueManagerImpl> {
  private final Provider<Logger> loggerProvider;

  public QueueManagerImpl_Factory(Provider<Logger> loggerProvider) {
    this.loggerProvider = loggerProvider;
  }

  @Override
  public QueueManagerImpl get() {
    return newInstance(loggerProvider.get());
  }

  public static QueueManagerImpl_Factory create(Provider<Logger> loggerProvider) {
    return new QueueManagerImpl_Factory(loggerProvider);
  }

  public static QueueManagerImpl newInstance(Logger logger) {
    return new QueueManagerImpl(logger);
  }
}
