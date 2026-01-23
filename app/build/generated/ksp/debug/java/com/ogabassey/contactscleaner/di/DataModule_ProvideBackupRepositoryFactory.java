package com.ogabassey.contactscleaner.di;

import com.ogabassey.contactscleaner.data.db.dao.UndoDao;
import com.ogabassey.contactscleaner.domain.repository.BackupRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.Providers;
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
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class DataModule_ProvideBackupRepositoryFactory implements Factory<BackupRepository> {
  private final Provider<UndoDao> undoDaoProvider;

  public DataModule_ProvideBackupRepositoryFactory(Provider<UndoDao> undoDaoProvider) {
    this.undoDaoProvider = undoDaoProvider;
  }

  @Override
  public BackupRepository get() {
    return provideBackupRepository(undoDaoProvider.get());
  }

  public static DataModule_ProvideBackupRepositoryFactory create(
      javax.inject.Provider<UndoDao> undoDaoProvider) {
    return new DataModule_ProvideBackupRepositoryFactory(Providers.asDaggerProvider(undoDaoProvider));
  }

  public static DataModule_ProvideBackupRepositoryFactory create(
      Provider<UndoDao> undoDaoProvider) {
    return new DataModule_ProvideBackupRepositoryFactory(undoDaoProvider);
  }

  public static BackupRepository provideBackupRepository(UndoDao undoDao) {
    return Preconditions.checkNotNullFromProvides(DataModule.INSTANCE.provideBackupRepository(undoDao));
  }
}
