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
public final class GetContactsPagedUseCase_Factory implements Factory<GetContactsPagedUseCase> {
  private final Provider<ContactRepository> contactRepositoryProvider;

  public GetContactsPagedUseCase_Factory(Provider<ContactRepository> contactRepositoryProvider) {
    this.contactRepositoryProvider = contactRepositoryProvider;
  }

  @Override
  public GetContactsPagedUseCase get() {
    return newInstance(contactRepositoryProvider.get());
  }

  public static GetContactsPagedUseCase_Factory create(
      javax.inject.Provider<ContactRepository> contactRepositoryProvider) {
    return new GetContactsPagedUseCase_Factory(Providers.asDaggerProvider(contactRepositoryProvider));
  }

  public static GetContactsPagedUseCase_Factory create(
      Provider<ContactRepository> contactRepositoryProvider) {
    return new GetContactsPagedUseCase_Factory(contactRepositoryProvider);
  }

  public static GetContactsPagedUseCase newInstance(ContactRepository contactRepository) {
    return new GetContactsPagedUseCase(contactRepository);
  }
}
