package com.ogabassey.contactscleaner.ui.dashboard;

import com.ogabassey.contactscleaner.data.util.ScanResultProvider;
import com.ogabassey.contactscleaner.domain.usecase.ScanContactsUseCase;
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
public final class DashboardViewModel_Factory implements Factory<DashboardViewModel> {
  private final Provider<ScanContactsUseCase> scanContactsUseCaseProvider;

  private final Provider<ScanResultProvider> scanResultProvider;

  public DashboardViewModel_Factory(Provider<ScanContactsUseCase> scanContactsUseCaseProvider,
      Provider<ScanResultProvider> scanResultProvider) {
    this.scanContactsUseCaseProvider = scanContactsUseCaseProvider;
    this.scanResultProvider = scanResultProvider;
  }

  @Override
  public DashboardViewModel get() {
    return newInstance(scanContactsUseCaseProvider.get(), scanResultProvider.get());
  }

  public static DashboardViewModel_Factory create(
      javax.inject.Provider<ScanContactsUseCase> scanContactsUseCaseProvider,
      javax.inject.Provider<ScanResultProvider> scanResultProvider) {
    return new DashboardViewModel_Factory(Providers.asDaggerProvider(scanContactsUseCaseProvider), Providers.asDaggerProvider(scanResultProvider));
  }

  public static DashboardViewModel_Factory create(
      Provider<ScanContactsUseCase> scanContactsUseCaseProvider,
      Provider<ScanResultProvider> scanResultProvider) {
    return new DashboardViewModel_Factory(scanContactsUseCaseProvider, scanResultProvider);
  }

  public static DashboardViewModel newInstance(ScanContactsUseCase scanContactsUseCase,
      ScanResultProvider scanResultProvider) {
    return new DashboardViewModel(scanContactsUseCase, scanResultProvider);
  }
}
