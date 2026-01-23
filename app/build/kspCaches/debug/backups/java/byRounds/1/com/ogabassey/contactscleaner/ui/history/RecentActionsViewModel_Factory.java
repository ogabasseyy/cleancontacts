package com.ogabassey.contactscleaner.ui.history;

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
public final class RecentActionsViewModel_Factory implements Factory<RecentActionsViewModel> {
  private final Provider<BackupRepository> backupRepositoryProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  public RecentActionsViewModel_Factory(Provider<BackupRepository> backupRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider) {
    this.backupRepositoryProvider = backupRepositoryProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
  }

  @Override
  public RecentActionsViewModel get() {
    return newInstance(backupRepositoryProvider.get(), contactRepositoryProvider.get());
  }

  public static RecentActionsViewModel_Factory create(
      javax.inject.Provider<BackupRepository> backupRepositoryProvider,
      javax.inject.Provider<ContactRepository> contactRepositoryProvider) {
    return new RecentActionsViewModel_Factory(Providers.asDaggerProvider(backupRepositoryProvider), Providers.asDaggerProvider(contactRepositoryProvider));
  }

  public static RecentActionsViewModel_Factory create(
      Provider<BackupRepository> backupRepositoryProvider,
      Provider<ContactRepository> contactRepositoryProvider) {
    return new RecentActionsViewModel_Factory(backupRepositoryProvider, contactRepositoryProvider);
  }

  public static RecentActionsViewModel newInstance(BackupRepository backupRepository,
      ContactRepository contactRepository) {
    return new RecentActionsViewModel(backupRepository, contactRepository);
  }
}
