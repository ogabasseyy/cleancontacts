package com.ogabassey.contactscleaner.domain.usecase;

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
public final class ScanContactsUseCase_Factory implements Factory<ScanContactsUseCase> {
  private final Provider<ContactRepository> contactRepositoryProvider;

  public ScanContactsUseCase_Factory(Provider<ContactRepository> contactRepositoryProvider) {
    this.contactRepositoryProvider = contactRepositoryProvider;
  }

  @Override
  public ScanContactsUseCase get() {
    return newInstance(contactRepositoryProvider.get());
  }

  public static ScanContactsUseCase_Factory create(
      javax.inject.Provider<ContactRepository> contactRepositoryProvider) {
    return new ScanContactsUseCase_Factory(Providers.asDaggerProvider(contactRepositoryProvider));
  }

  public static ScanContactsUseCase_Factory create(
      Provider<ContactRepository> contactRepositoryProvider) {
    return new ScanContactsUseCase_Factory(contactRepositoryProvider);
  }

  public static ScanContactsUseCase newInstance(ContactRepository contactRepository) {
    return new ScanContactsUseCase(contactRepository);
  }
}
