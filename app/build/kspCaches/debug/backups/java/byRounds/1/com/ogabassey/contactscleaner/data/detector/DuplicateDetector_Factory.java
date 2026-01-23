package com.ogabassey.contactscleaner.data.detector;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class DuplicateDetector_Factory implements Factory<DuplicateDetector> {
  private final Provider<Context> contextProvider;

  public DuplicateDetector_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public DuplicateDetector get() {
    return newInstance(contextProvider.get());
  }

  public static DuplicateDetector_Factory create(javax.inject.Provider<Context> contextProvider) {
    return new DuplicateDetector_Factory(Providers.asDaggerProvider(contextProvider));
  }

  public static DuplicateDetector_Factory create(Provider<Context> contextProvider) {
    return new DuplicateDetector_Factory(contextProvider);
  }

  public static DuplicateDetector newInstance(Context context) {
    return new DuplicateDetector(context);
  }
}
