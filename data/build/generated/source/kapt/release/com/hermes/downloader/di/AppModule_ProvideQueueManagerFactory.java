package com.hermes.downloader.di;

import com.hermes.downloader.core.Logger;
import com.hermes.downloader.domain.queue.QueueManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideQueueManagerFactory implements Factory<QueueManager> {
  private final Provider<Logger> loggerProvider;

  public AppModule_ProvideQueueManagerFactory(Provider<Logger> loggerProvider) {
    this.loggerProvider = loggerProvider;
  }

  @Override
  public QueueManager get() {
    return provideQueueManager(loggerProvider.get());
  }

  public static AppModule_ProvideQueueManagerFactory create(Provider<Logger> loggerProvider) {
    return new AppModule_ProvideQueueManagerFactory(loggerProvider);
  }

  public static QueueManager provideQueueManager(Logger logger) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideQueueManager(logger));
  }
}
