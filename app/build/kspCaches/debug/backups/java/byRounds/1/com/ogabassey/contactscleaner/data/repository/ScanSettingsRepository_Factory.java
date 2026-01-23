package com.ogabassey.contactscleaner.data.repository;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class ScanSettingsRepository_Factory implements Factory<ScanSettingsRepository> {
  private final Provider<Context> contextProvider;

  public ScanSettingsRepository_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ScanSettingsRepository get() {
    return newInstance(contextProvider.get());
  }

  public static ScanSettingsRepository_Factory create(
      javax.inject.Provider<Context> contextProvider) {
    return new ScanSettingsRepository_Factory(Providers.asDaggerProvider(contextProvider));
  }

  public static ScanSettingsRepository_Factory create(Provider<Context> contextProvider) {
    return new ScanSettingsRepository_Factory(contextProvider);
  }

  public static ScanSettingsRepository newInstance(Context context) {
    return new ScanSettingsRepository(context);
  }
}
