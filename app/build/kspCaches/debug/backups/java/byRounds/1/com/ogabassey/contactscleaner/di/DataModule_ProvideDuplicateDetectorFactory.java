package com.ogabassey.contactscleaner.di;

import com.ogabassey.contactscleaner.data.detector.DuplicateDetector;
import com.ogabassey.contactscleaner.data.provider.RegionProvider;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DataModule_ProvideDuplicateDetectorFactory implements Factory<DuplicateDetector> {
  private final Provider<RegionProvider> regionProvider;

  public DataModule_ProvideDuplicateDetectorFactory(Provider<RegionProvider> regionProvider) {
    this.regionProvider = regionProvider;
  }

  @Override
  public DuplicateDetector get() {
    return provideDuplicateDetector(regionProvider.get());
  }

  public static DataModule_ProvideDuplicateDetectorFactory create(
      javax.inject.Provider<RegionProvider> regionProvider) {
    return new DataModule_ProvideDuplicateDetectorFactory(Providers.asDaggerProvider(regionProvider));
  }

  public static DataModule_ProvideDuplicateDetectorFactory create(
      Provider<RegionProvider> regionProvider) {
    return new DataModule_ProvideDuplicateDetectorFactory(regionProvider);
  }

  public static DuplicateDetector provideDuplicateDetector(RegionProvider regionProvider) {
    return Preconditions.checkNotNullFromProvides(DataModule.INSTANCE.provideDuplicateDetector(regionProvider));
  }
}
