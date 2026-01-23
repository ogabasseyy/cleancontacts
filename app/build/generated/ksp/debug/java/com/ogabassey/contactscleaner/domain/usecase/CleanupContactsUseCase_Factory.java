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
public final class CleanupContactsUseCase_Factory implements Factory<CleanupContactsUseCase> {
  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<BackupRepository> backupRepositoryProvider;

  public CleanupContactsUseCase_Factory(Provider<ContactRepository> contactRepositoryProvider,
      Provider<BackupRepository> backupRepositoryProvider) {
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.backupRepositoryProvider = backupRepositoryProvider;
  }

  @Override
  public CleanupContactsUseCase get() {
    return newInstance(contactRepositoryProvider.get(), backupRepositoryProvider.get());
  }

  public static CleanupContactsUseCase_Factory create(
      javax.inject.Provider<ContactRepository> contactRepositoryProvider,
      javax.inject.Provider<BackupRepository> backupRepositoryProvider) {
    return new CleanupContactsUseCase_Factory(Providers.asDaggerProvider(contactRepositoryProvider), Providers.asDaggerProvider(backupRepositoryProvider));
  }

  public static CleanupContactsUseCase_Factory create(
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<BackupRepository> backupRepositoryProvider) {
    return new CleanupContactsUseCase_Factory(contactRepositoryProvider, backupRepositoryProvider);
  }

  public static CleanupContactsUseCase newInstance(ContactRepository contactRepository,
      BackupRepository backupRepository) {
    return new CleanupContactsUseCase(contactRepository, backupRepository);
  }
}
