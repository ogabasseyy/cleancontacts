package com.ogabassey.contactscleaner.di;

import android.content.Context;
import com.ogabassey.contactscleaner.data.detector.DuplicateDetector;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
  private final Provider<Context> contextProvider;

  public DataModule_ProvideDuplicateDetectorFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public DuplicateDetector get() {
    return provideDuplicateDetector(contextProvider.get());
  }

  public static DataModule_ProvideDuplicateDetectorFactory create(
      javax.inject.Provider<Context> contextProvider) {
    return new DataModule_ProvideDuplicateDetectorFactory(Providers.asDaggerProvider(contextProvider));
  }

  public static DataModule_ProvideDuplicateDetectorFactory create(
      Provider<Context> contextProvider) {
    return new DataModule_ProvideDuplicateDetectorFactory(contextProvider);
  }

  public static DuplicateDetector provideDuplicateDetector(Context context) {
    return Preconditions.checkNotNullFromProvides(DataModule.INSTANCE.provideDuplicateDetector(context));
  }
}
