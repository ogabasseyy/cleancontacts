package com.ogabassey.contactscleaner.di;

import android.content.Context;
import com.ogabassey.contactscleaner.domain.repository.FileService;
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
public final class DataModule_ProvideFileServiceFactory implements Factory<FileService> {
  private final Provider<Context> contextProvider;

  public DataModule_ProvideFileServiceFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public FileService get() {
    return provideFileService(contextProvider.get());
  }

  public static DataModule_ProvideFileServiceFactory create(
      javax.inject.Provider<Context> contextProvider) {
    return new DataModule_ProvideFileServiceFactory(Providers.asDaggerProvider(contextProvider));
  }

  public static DataModule_ProvideFileServiceFactory create(Provider<Context> contextProvider) {
    return new DataModule_ProvideFileServiceFactory(contextProvider);
  }

  public static FileService provideFileService(Context context) {
    return Preconditions.checkNotNullFromProvides(DataModule.INSTANCE.provideFileService(context));
  }
}
