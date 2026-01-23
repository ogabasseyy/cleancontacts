package com.ogabassey.contactscleaner.di;

import android.content.Context;
import com.ogabassey.contactscleaner.data.source.ContactsProviderSource;
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
public final class DataModule_ProvideContactsProviderSourceFactory implements Factory<ContactsProviderSource> {
  private final Provider<Context> contextProvider;

  public DataModule_ProvideContactsProviderSourceFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ContactsProviderSource get() {
    return provideContactsProviderSource(contextProvider.get());
  }

  public static DataModule_ProvideContactsProviderSourceFactory create(
      javax.inject.Provider<Context> contextProvider) {
    return new DataModule_ProvideContactsProviderSourceFactory(Providers.asDaggerProvider(contextProvider));
  }

  public static DataModule_ProvideContactsProviderSourceFactory create(
      Provider<Context> contextProvider) {
    return new DataModule_ProvideContactsProviderSourceFactory(contextProvider);
  }

  public static ContactsProviderSource provideContactsProviderSource(Context context) {
    return Preconditions.checkNotNullFromProvides(DataModule.INSTANCE.provideContactsProviderSource(context));
  }
}
