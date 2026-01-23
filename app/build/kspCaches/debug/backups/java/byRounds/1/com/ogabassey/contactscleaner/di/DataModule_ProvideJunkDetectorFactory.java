package com.ogabassey.contactscleaner.di;

import com.ogabassey.contactscleaner.data.detector.JunkDetector;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DataModule_ProvideJunkDetectorFactory implements Factory<JunkDetector> {
  @Override
  public JunkDetector get() {
    return provideJunkDetector();
  }

  public static DataModule_ProvideJunkDetectorFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static JunkDetector provideJunkDetector() {
    return Preconditions.checkNotNullFromProvides(DataModule.INSTANCE.provideJunkDetector());
  }

  private static final class InstanceHolder {
    static final DataModule_ProvideJunkDetectorFactory INSTANCE = new DataModule_ProvideJunkDetectorFactory();
  }
}
