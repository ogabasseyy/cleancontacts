package com.ogabassey.contactscleaner.data.repository;

import com.ogabassey.contactscleaner.data.db.dao.ContactDao;
import com.ogabassey.contactscleaner.data.db.dao.IgnoredContactDao;
import com.ogabassey.contactscleaner.data.detector.DuplicateDetector;
import com.ogabassey.contactscleaner.data.detector.FormatDetector;
import com.ogabassey.contactscleaner.data.detector.JunkDetector;
import com.ogabassey.contactscleaner.data.detector.SensitiveDataDetector;
import com.ogabassey.contactscleaner.data.source.ContactsProviderSource;
import com.ogabassey.contactscleaner.data.util.ScanResultProvider;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
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
public final class ContactRepositoryImpl_Factory implements Factory<ContactRepositoryImpl> {
  private final Provider<ContactDao> contactDaoProvider;

  private final Provider<ContactsProviderSource> contactsProviderSourceProvider;

  private final Provider<JunkDetector> junkDetectorProvider;

  private final Provider<DuplicateDetector> duplicateDetectorProvider;

  private final Provider<FormatDetector> formatDetectorProvider;

  private final Provider<SensitiveDataDetector> sensitiveDetectorProvider;

  private final Provider<IgnoredContactDao> ignoredContactDaoProvider;

  private final Provider<ScanResultProvider> scanResultProvider;

  public ContactRepositoryImpl_Factory(Provider<ContactDao> contactDaoProvider,
      Provider<ContactsProviderSource> contactsProviderSourceProvider,
      Provider<JunkDetector> junkDetectorProvider,
      Provider<DuplicateDetector> duplicateDetectorProvider,
      Provider<FormatDetector> formatDetectorProvider,
      Provider<SensitiveDataDetector> sensitiveDetectorProvider,
      Provider<IgnoredContactDao> ignoredContactDaoProvider,
      Provider<ScanResultProvider> scanResultProvider) {
    this.contactDaoProvider = contactDaoProvider;
    this.contactsProviderSourceProvider = contactsProviderSourceProvider;
    this.junkDetectorProvider = junkDetectorProvider;
    this.duplicateDetectorProvider = duplicateDetectorProvider;
    this.formatDetectorProvider = formatDetectorProvider;
    this.sensitiveDetectorProvider = sensitiveDetectorProvider;
    this.ignoredContactDaoProvider = ignoredContactDaoProvider;
    this.scanResultProvider = scanResultProvider;
  }

  @Override
  public ContactRepositoryImpl get() {
    return newInstance(contactDaoProvider.get(), contactsProviderSourceProvider.get(), junkDetectorProvider.get(), duplicateDetectorProvider.get(), formatDetectorProvider.get(), sensitiveDetectorProvider.get(), ignoredContactDaoProvider.get(), scanResultProvider.get());
  }

  public static ContactRepositoryImpl_Factory create(
      javax.inject.Provider<ContactDao> contactDaoProvider,
      javax.inject.Provider<ContactsProviderSource> contactsProviderSourceProvider,
      javax.inject.Provider<JunkDetector> junkDetectorProvider,
      javax.inject.Provider<DuplicateDetector> duplicateDetectorProvider,
      javax.inject.Provider<FormatDetector> formatDetectorProvider,
      javax.inject.Provider<SensitiveDataDetector> sensitiveDetectorProvider,
      javax.inject.Provider<IgnoredContactDao> ignoredContactDaoProvider,
      javax.inject.Provider<ScanResultProvider> scanResultProvider) {
    return new ContactRepositoryImpl_Factory(Providers.asDaggerProvider(contactDaoProvider), Providers.asDaggerProvider(contactsProviderSourceProvider), Providers.asDaggerProvider(junkDetectorProvider), Providers.asDaggerProvider(duplicateDetectorProvider), Providers.asDaggerProvider(formatDetectorProvider), Providers.asDaggerProvider(sensitiveDetectorProvider), Providers.asDaggerProvider(ignoredContactDaoProvider), Providers.asDaggerProvider(scanResultProvider));
  }

  public static ContactRepositoryImpl_Factory create(Provider<ContactDao> contactDaoProvider,
      Provider<ContactsProviderSource> contactsProviderSourceProvider,
      Provider<JunkDetector> junkDetectorProvider,
      Provider<DuplicateDetector> duplicateDetectorProvider,
      Provider<FormatDetector> formatDetectorProvider,
      Provider<SensitiveDataDetector> sensitiveDetectorProvider,
      Provider<IgnoredContactDao> ignoredContactDaoProvider,
      Provider<ScanResultProvider> scanResultProvider) {
    return new ContactRepositoryImpl_Factory(contactDaoProvider, contactsProviderSourceProvider, junkDetectorProvider, duplicateDetectorProvider, formatDetectorProvider, sensitiveDetectorProvider, ignoredContactDaoProvider, scanResultProvider);
  }

  public static ContactRepositoryImpl newInstance(ContactDao contactDao,
      ContactsProviderSource contactsProviderSource, JunkDetector junkDetector,
      DuplicateDetector duplicateDetector, FormatDetector formatDetector,
      SensitiveDataDetector sensitiveDetector, IgnoredContactDao ignoredContactDao,
      ScanResultProvider scanResultProvider) {
    return new ContactRepositoryImpl(contactDao, contactsProviderSource, junkDetector, duplicateDetector, formatDetector, sensitiveDetector, ignoredContactDao, scanResultProvider);
  }
}
