package com.ogabassey.contactscleaner.data.source;

import android.content.ContentResolver;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
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
public final class ContactsProviderSource_Factory implements Factory<ContactsProviderSource> {
  private final Provider<ContentResolver> contentResolverProvider;

  public ContactsProviderSource_Factory(Provider<ContentResolver> contentResolverProvider) {
    this.contentResolverProvider = contentResolverProvider;
  }

  @Override
  public ContactsProviderSource get() {
    return newInstance(contentResolverProvider.get());
  }

  public static ContactsProviderSource_Factory create(
      javax.inject.Provider<ContentResolver> contentResolverProvider) {
    return new ContactsProviderSource_Factory(Providers.asDaggerProvider(contentResolverProvider));
  }

  public static ContactsProviderSource_Factory create(
      Provider<ContentResolver> contentResolverProvider) {
    return new ContactsProviderSource_Factory(contentResolverProvider);
  }

  public static ContactsProviderSource newInstance(ContentResolver contentResolver) {
    return new ContactsProviderSource(contentResolver);
  }
}
