package com.ogabassey.contactscleaner.data.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.ogabassey.contactscleaner.data.db.dao.ContactDao;
import com.ogabassey.contactscleaner.data.detector.JunkDetector;
import com.ogabassey.contactscleaner.data.source.ContactsProviderSource;
import dagger.internal.DaggerGenerated;
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
public final class ContactSyncWorker_Factory {
  private final Provider<ContactDao> contactDaoProvider;

  private final Provider<ContactsProviderSource> contactsSourceProvider;

  private final Provider<JunkDetector> junkDetectorProvider;

  public ContactSyncWorker_Factory(Provider<ContactDao> contactDaoProvider,
      Provider<ContactsProviderSource> contactsSourceProvider,
      Provider<JunkDetector> junkDetectorProvider) {
    this.contactDaoProvider = contactDaoProvider;
    this.contactsSourceProvider = contactsSourceProvider;
    this.junkDetectorProvider = junkDetectorProvider;
  }

  public ContactSyncWorker get(Context appContext, WorkerParameters workerParams) {
    return newInstance(appContext, workerParams, contactDaoProvider.get(), contactsSourceProvider.get(), junkDetectorProvider.get());
  }

  public static ContactSyncWorker_Factory create(
      javax.inject.Provider<ContactDao> contactDaoProvider,
      javax.inject.Provider<ContactsProviderSource> contactsSourceProvider,
      javax.inject.Provider<JunkDetector> junkDetectorProvider) {
    return new ContactSyncWorker_Factory(Providers.asDaggerProvider(contactDaoProvider), Providers.asDaggerProvider(contactsSourceProvider), Providers.asDaggerProvider(junkDetectorProvider));
  }

  public static ContactSyncWorker_Factory create(Provider<ContactDao> contactDaoProvider,
      Provider<ContactsProviderSource> contactsSourceProvider,
      Provider<JunkDetector> junkDetectorProvider) {
    return new ContactSyncWorker_Factory(contactDaoProvider, contactsSourceProvider, junkDetectorProvider);
  }

  public static ContactSyncWorker newInstance(Context appContext, WorkerParameters workerParams,
      ContactDao contactDao, ContactsProviderSource contactsSource, JunkDetector junkDetector) {
    return new ContactSyncWorker(appContext, workerParams, contactDao, contactsSource, junkDetector);
  }
}
