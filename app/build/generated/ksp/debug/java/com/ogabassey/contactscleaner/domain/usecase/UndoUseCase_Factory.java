package com.ogabassey.contactscleaner.domain.usecase;

import com.ogabassey.contactscleaner.domain.repository.BackupRepository;
import com.ogabassey.contactscleaner.domain.repository.ContactRepository;
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
public final class UndoUseCase_Factory implements Factory<UndoUseCase> {
  private final Provider<BackupRepository> backupRepositoryProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  public UndoUseCase_Factory(Provider<BackupRepository> backupRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider) {
    this.backupRepositoryProvider = backupRepositoryProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
  }

  @Override
  public UndoUseCase get() {
    return newInstance(backupRepositoryProvider.get(), contactRepositoryProvider.get());
  }

  public static UndoUseCase_Factory create(
      javax.inject.Provider<BackupRepository> backupRepositoryProvider,
      javax.inject.Provider<ContactRepository> contactRepositoryProvider) {
    return new UndoUseCase_Factory(Providers.asDaggerProvider(backupRepositoryProvider), Providers.asDaggerProvider(contactRepositoryProvider));
  }

  public static UndoUseCase_Factory create(Provider<BackupRepository> backupRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider) {
    return new UndoUseCase_Factory(backupRepositoryProvider, contactRepositoryProvider);
  }

  public static UndoUseCase newInstance(BackupRepository backupRepository,
      ContactRepository contactRepository) {
    return new UndoUseCase(backupRepository, contactRepository);
  }
}
