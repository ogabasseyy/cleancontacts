package com.ogabassey.contactscleaner.data.repository;

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
public final class FileServiceImpl_Factory implements Factory<FileServiceImpl> {
  private final Provider<Context> contextProvider;

  public FileServiceImpl_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public FileServiceImpl get() {
    return newInstance(contextProvider.get());
  }

  public static FileServiceImpl_Factory create(javax.inject.Provider<Context> contextProvider) {
    return new FileServiceImpl_Factory(Providers.asDaggerProvider(contextProvider));
  }

  public static FileServiceImpl_Factory create(Provider<Context> contextProvider) {
    return new FileServiceImpl_Factory(contextProvider);
  }

  public static FileServiceImpl newInstance(Context context) {
    return new FileServiceImpl(context);
  }
}
