package com.ogabassey.contactscleaner.ui.results;

import com.ogabassey.contactscleaner.data.detector.FormatDetector;
import com.ogabassey.contactscleaner.data.repository.UsageRepository;
import com.ogabassey.contactscleaner.data.util.ScanResultProvider;
import com.ogabassey.contactscleaner.domain.repository.BillingRepository;
import com.ogabassey.contactscleaner.domain.repository.ContactRepository;
import com.ogabassey.contactscleaner.domain.usecase.CleanupContactsUseCase;
import com.ogabassey.contactscleaner.domain.usecase.ExportUseCase;
import com.ogabassey.contactscleaner.domain.usecase.GetContactsPagedUseCase;
import com.ogabassey.contactscleaner.domain.usecase.UndoUseCase;
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
public final class ResultsViewModel_Factory implements Factory<ResultsViewModel> {
  private final Provider<GetContactsPagedUseCase> getContactsPagedUseCaseProvider;

  private final Provider<CleanupContactsUseCase> cleanupContactsUseCaseProvider;

  private final Provider<ScanResultProvider> scanResultProvider;

  private final Provider<ContactRepository> contactRepositoryProvider;

  private final Provider<BillingRepository> billingRepositoryProvider;

  private final Provider<FormatDetector> formatDetectorProvider;

  private final Provider<ExportUseCase> exportUseCaseProvider;

  private final Provider<UndoUseCase> undoUseCaseProvider;

  private final Provider<UsageRepository> usageRepositoryProvider;

  public ResultsViewModel_Factory(Provider<GetContactsPagedUseCase> getContactsPagedUseCaseProvider,
      Provider<CleanupContactsUseCase> cleanupContactsUseCaseProvider,
      Provider<ScanResultProvider> scanResultProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<BillingRepository> billingRepositoryProvider,
      Provider<FormatDetector> formatDetectorProvider,
      Provider<ExportUseCase> exportUseCaseProvider, Provider<UndoUseCase> undoUseCaseProvider,
      Provider<UsageRepository> usageRepositoryProvider) {
    this.getContactsPagedUseCaseProvider = getContactsPagedUseCaseProvider;
    this.cleanupContactsUseCaseProvider = cleanupContactsUseCaseProvider;
    this.scanResultProvider = scanResultProvider;
    this.contactRepositoryProvider = contactRepositoryProvider;
    this.billingRepositoryProvider = billingRepositoryProvider;
    this.formatDetectorProvider = formatDetectorProvider;
    this.exportUseCaseProvider = exportUseCaseProvider;
    this.undoUseCaseProvider = undoUseCaseProvider;
    this.usageRepositoryProvider = usageRepositoryProvider;
  }

  @Override
  public ResultsViewModel get() {
    return newInstance(getContactsPagedUseCaseProvider.get(), cleanupContactsUseCaseProvider.get(), scanResultProvider.get(), contactRepositoryProvider.get(), billingRepositoryProvider.get(), formatDetectorProvider.get(), exportUseCaseProvider.get(), undoUseCaseProvider.get(), usageRepositoryProvider.get());
  }

  public static ResultsViewModel_Factory create(
      javax.inject.Provider<GetContactsPagedUseCase> getContactsPagedUseCaseProvider,
      javax.inject.Provider<CleanupContactsUseCase> cleanupContactsUseCaseProvider,
      javax.inject.Provider<ScanResultProvider> scanResultProvider,
      javax.inject.Provider<ContactRepository> contactRepositoryProvider,
      javax.inject.Provider<BillingRepository> billingRepositoryProvider,
      javax.inject.Provider<FormatDetector> formatDetectorProvider,
      javax.inject.Provider<ExportUseCase> exportUseCaseProvider,
      javax.inject.Provider<UndoUseCase> undoUseCaseProvider,
      javax.inject.Provider<UsageRepository> usageRepositoryProvider) {
    return new ResultsViewModel_Factory(Providers.asDaggerProvider(getContactsPagedUseCaseProvider), Providers.asDaggerProvider(cleanupContactsUseCaseProvider), Providers.asDaggerProvider(scanResultProvider), Providers.asDaggerProvider(contactRepositoryProvider), Providers.asDaggerProvider(billingRepositoryProvider), Providers.asDaggerProvider(formatDetectorProvider), Providers.asDaggerProvider(exportUseCaseProvider), Providers.asDaggerProvider(undoUseCaseProvider), Providers.asDaggerProvider(usageRepositoryProvider));
  }

  public static ResultsViewModel_Factory create(
      Provider<GetContactsPagedUseCase> getContactsPagedUseCaseProvider,
      Provider<CleanupContactsUseCase> cleanupContactsUseCaseProvider,
      Provider<ScanResultProvider> scanResultProvider,
      Provider<ContactRepository> contactRepositoryProvider,
      Provider<BillingRepository> billingRepositoryProvider,
      Provider<FormatDetector> formatDetectorProvider,
      Provider<ExportUseCase> exportUseCaseProvider, Provider<UndoUseCase> undoUseCaseProvider,
      Provider<UsageRepository> usageRepositoryProvider) {
    return new ResultsViewModel_Factory(getContactsPagedUseCaseProvider, cleanupContactsUseCaseProvider, scanResultProvider, contactRepositoryProvider, billingRepositoryProvider, formatDetectorProvider, exportUseCaseProvider, undoUseCaseProvider, usageRepositoryProvider);
  }

  public static ResultsViewModel newInstance(GetContactsPagedUseCase getContactsPagedUseCase,
      CleanupContactsUseCase cleanupContactsUseCase, ScanResultProvider scanResultProvider,
      ContactRepository contactRepository, BillingRepository billingRepository,
      FormatDetector formatDetector, ExportUseCase exportUseCase, UndoUseCase undoUseCase,
      UsageRepository usageRepository) {
    return new ResultsViewModel(getContactsPagedUseCase, cleanupContactsUseCase, scanResultProvider, contactRepository, billingRepository, formatDetector, exportUseCase, undoUseCase, usageRepository);
  }
}
