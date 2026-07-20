package com.hermes.downloader.di;

import com.hermes.downloader.data.local.TaskDao;
import com.hermes.downloader.data.local.YTDowDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class DatabaseModule_ProvideTaskDaoFactory implements Factory<TaskDao> {
  private final Provider<YTDowDatabase> dbProvider;

  public DatabaseModule_ProvideTaskDaoFactory(Provider<YTDowDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public TaskDao get() {
    return provideTaskDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideTaskDaoFactory create(Provider<YTDowDatabase> dbProvider) {
    return new DatabaseModule_ProvideTaskDaoFactory(dbProvider);
  }

  public static TaskDao provideTaskDao(YTDowDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideTaskDao(db));
  }
}
