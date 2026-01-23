package com.ogabassey.contactscleaner.domain.usecase;

import com.ogabassey.contactscleaner.domain.repository.ContactRepository;
import com.ogabassey.contactscleaner.domain.repository.FileService;
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
public final class ExportUseCase_Factory implements Factory<ExportUseCase> {
  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<FileService> fileServiceProvider;

  public ExportUseCase_Factory(Provider<ContactRepository> contactRepositoryProvider,
      Provider<FileService> fileServiceProvider) {
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.fileServiceProvider = fileServiceProvider;
  }

  @Override
  public ExportUseCase get() {
    return newInstance(contactRepositoryProvider.get(), fileServiceProvider.get());
  }

  public static ExportUseCase_Factory create(
      javax.inject.Provider<ContactRepository> contactRepositoryProvider,
      javax.inject.Provider<FileService> fileServiceProvider) {
    return new ExportUseCase_Factory(Providers.asDaggerProvider(contactRepositoryProvider), Providers.asDaggerProvider(fileServiceProvider));
  }

  public static ExportUseCase_Factory create(Provider<ContactRepository> contactRepositoryProvider,
      Provider<FileService> fileServiceProvider) {
    return new ExportUseCase_Factory(contactRepositoryProvider, fileServiceProvider);
  }

  public static ExportUseCase newInstance(ContactRepository contactRepository,
      FileService fileService) {
    return new ExportUseCase(contactRepository, fileService);
  }
}
