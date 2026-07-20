package com.hermes.downloader.di;

import com.hermes.downloader.data.local.HistoryDao;
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
public final class DatabaseModule_ProvideHistoryDaoFactory implements Factory<HistoryDao> {
  private final Provider<YTDowDatabase> dbProvider;

  public DatabaseModule_ProvideHistoryDaoFactory(Provider<YTDowDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public HistoryDao get() {
    return provideHistoryDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideHistoryDaoFactory create(Provider<YTDowDatabase> dbProvider) {
    return new DatabaseModule_ProvideHistoryDaoFactory(dbProvider);
  }

  public static HistoryDao provideHistoryDao(YTDowDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideHistoryDao(db));
  }
}
