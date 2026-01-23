package com.ogabassey.contactscleaner.data.detector;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class JunkDetector_Factory implements Factory<JunkDetector> {
  @Override
  public JunkDetector get() {
    return newInstance();
  }

  public static JunkDetector_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static JunkDetector newInstance() {
    return new JunkDetector();
  }

  private static final class InstanceHolder {
    static final JunkDetector_Factory INSTANCE = new JunkDetector_Factory();
  }
}
