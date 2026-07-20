package com.hermes.downloader.di;

import com.hermes.downloader.core.Logger;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class AppModule_ProvideLoggerFactory implements Factory<Logger> {
  @Override
  public Logger get() {
    return provideLogger();
  }

  public static AppModule_ProvideLoggerFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static Logger provideLogger() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideLogger());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideLoggerFactory INSTANCE = new AppModule_ProvideLoggerFactory();
  }
}
