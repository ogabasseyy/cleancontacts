package com.ogabassey.contactscleaner.data.repository;

import com.ogabassey.contactscleaner.data.db.dao.UndoDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class BackupRepositoryImpl_Factory implements Factory<BackupRepositoryImpl> {
  private final Provider<UndoDao> undoDaoProvider;

  public BackupRepositoryImpl_Factory(Provider<UndoDao> undoDaoProvider) {
    this.undoDaoProvider = undoDaoProvider;
  }

  @Override
  public BackupRepositoryImpl get() {
    return newInstance(undoDaoProvider.get());
  }

  public static BackupRepositoryImpl_Factory create(
      javax.inject.Provider<UndoDao> undoDaoProvider) {
    return new BackupRepositoryImpl_Factory(Providers.asDaggerProvider(undoDaoProvider));
  }

  public static BackupRepositoryImpl_Factory create(Provider<UndoDao> undoDaoProvider) {
    return new BackupRepositoryImpl_Factory(undoDaoProvider);
  }

  public static BackupRepositoryImpl newInstance(UndoDao undoDao) {
    return new BackupRepositoryImpl(undoDao);
  }
}
