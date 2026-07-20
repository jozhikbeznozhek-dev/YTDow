package com.hermes.downloader.di;

import com.hermes.downloader.domain.repository.UpdateRepository;
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
public final class RepositoryModule_ProvideUpdateRepositoryFactory implements Factory<UpdateRepository> {
  @Override
  public UpdateRepository get() {
    return provideUpdateRepository();
  }

  public static RepositoryModule_ProvideUpdateRepositoryFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static UpdateRepository provideUpdateRepository() {
    return Preconditions.checkNotNullFromProvides(RepositoryModule.INSTANCE.provideUpdateRepository());
  }

  private static final class InstanceHolder {
    private static final RepositoryModule_ProvideUpdateRepositoryFactory INSTANCE = new RepositoryModule_ProvideUpdateRepositoryFactory();
  }
}
