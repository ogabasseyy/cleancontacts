package com.ogabassey.contactscleaner.data.detector;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class FormatDetector_Factory implements Factory<FormatDetector> {
  @Override
  public FormatDetector get() {
    return newInstance();
  }

  public static FormatDetector_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static FormatDetector newInstance() {
    return new FormatDetector();
  }

  private static final class InstanceHolder {
    static final FormatDetector_Factory INSTANCE = new FormatDetector_Factory();
  }
}
