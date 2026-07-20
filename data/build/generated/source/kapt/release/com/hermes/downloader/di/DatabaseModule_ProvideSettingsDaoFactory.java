package com.hermes.downloader.di;

import com.hermes.downloader.data.local.SettingsDao;
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
public final class DatabaseModule_ProvideSettingsDaoFactory implements Factory<SettingsDao> {
  private final Provider<YTDowDatabase> dbProvider;

  public DatabaseModule_ProvideSettingsDaoFactory(Provider<YTDowDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public SettingsDao get() {
    return provideSettingsDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideSettingsDaoFactory create(
      Provider<YTDowDatabase> dbProvider) {
    return new DatabaseModule_ProvideSettingsDaoFactory(dbProvider);
  }

  public static SettingsDao provideSettingsDao(YTDowDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideSettingsDao(db));
  }
}
