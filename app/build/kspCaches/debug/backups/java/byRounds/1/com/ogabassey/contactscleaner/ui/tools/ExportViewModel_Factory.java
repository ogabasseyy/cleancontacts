package com.ogabassey.contactscleaner.ui.tools;

import com.ogabassey.contactscleaner.domain.usecase.ExportUseCase;
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
public final class ExportViewModel_Factory implements Factory<ExportViewModel> {
  private final Provider<ExportUseCase> exportUseCaseProvider;

  public ExportViewModel_Factory(Provider<ExportUseCase> exportUseCaseProvider) {
    this.exportUseCaseProvider = exportUseCaseProvider;
  }

  @Override
  public ExportViewModel get() {
    return newInstance(exportUseCaseProvider.get());
  }

  public static ExportViewModel_Factory create(
      javax.inject.Provider<ExportUseCase> exportUseCaseProvider) {
    return new ExportViewModel_Factory(Providers.asDaggerProvider(exportUseCaseProvider));
  }

  public static ExportViewModel_Factory create(Provider<ExportUseCase> exportUseCaseProvider) {
    return new ExportViewModel_Factory(exportUseCaseProvider);
  }

  public static ExportViewModel newInstance(ExportUseCase exportUseCase) {
    return new ExportViewModel(exportUseCase);
  }
}
