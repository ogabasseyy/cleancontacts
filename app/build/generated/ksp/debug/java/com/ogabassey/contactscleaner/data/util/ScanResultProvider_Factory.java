package com.ogabassey.contactscleaner.data.util;

import com.ogabassey.contactscleaner.data.repository.ScanSettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class ScanResultProvider_Factory implements Factory<ScanResultProvider> {
  private final Provider<ScanSettingsRepository> scanSettingsRepositoryProvider;

  public ScanResultProvider_Factory(
      Provider<ScanSettingsRepository> scanSettingsRepositoryProvider) {
    this.scanSettingsRepositoryProvider = scanSettingsRepositoryProvider;
  }

  @Override
  public ScanResultProvider get() {
    return newInstance(scanSettingsRepositoryProvider.get());
  }

  public static ScanResultProvider_Factory create(
      javax.inject.Provider<ScanSettingsRepository> scanSettingsRepositoryProvider) {
    return new ScanResultProvider_Factory(Providers.asDaggerProvider(scanSettingsRepositoryProvider));
  }

  public static ScanResultProvider_Factory create(
      Provider<ScanSettingsRepository> scanSettingsRepositoryProvider) {
    return new ScanResultProvider_Factory(scanSettingsRepositoryProvider);
  }

  public static ScanResultProvider newInstance(ScanSettingsRepository scanSettingsRepository) {
    return new ScanResultProvider(scanSettingsRepository);
  }
}
