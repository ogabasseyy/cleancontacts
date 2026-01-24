package com.ogabassey.contactscleaner.data.detector;

import com.ogabassey.contactscleaner.data.provider.RegionProvider;
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
public final class DuplicateDetector_Factory implements Factory<DuplicateDetector> {
  private final Provider<RegionProvider> regionProvider;

  public DuplicateDetector_Factory(Provider<RegionProvider> regionProvider) {
    this.regionProvider = regionProvider;
  }

  @Override
  public DuplicateDetector get() {
    return newInstance(regionProvider.get());
  }

  public static DuplicateDetector_Factory create(
      javax.inject.Provider<RegionProvider> regionProvider) {
    return new DuplicateDetector_Factory(Providers.asDaggerProvider(regionProvider));
  }

  public static DuplicateDetector_Factory create(Provider<RegionProvider> regionProvider) {
    return new DuplicateDetector_Factory(regionProvider);
  }

  public static DuplicateDetector newInstance(RegionProvider regionProvider) {
    return new DuplicateDetector(regionProvider);
  }
}
