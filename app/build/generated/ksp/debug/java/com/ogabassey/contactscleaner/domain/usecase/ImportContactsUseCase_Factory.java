package com.ogabassey.contactscleaner.domain.usecase;

import com.ogabassey.contactscleaner.data.detector.DuplicateDetector;
import com.ogabassey.contactscleaner.data.detector.JunkDetector;
import com.ogabassey.contactscleaner.data.parser.ContactImportParser;
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
public final class ImportContactsUseCase_Factory implements Factory<ImportContactsUseCase> {
  private final Provider<ContactImportParser> contactImportParserProvider;

  private final Provider<JunkDetector> junkDetectorProvider;

  private final Provider<DuplicateDetector> duplicateDetectorProvider;

  public ImportContactsUseCase_Factory(Provider<ContactImportParser> contactImportParserProvider,
      Provider<JunkDetector> junkDetectorProvider,
      Provider<DuplicateDetector> duplicateDetectorProvider) {
    this.contactImportParserProvider = contactImportParserProvider;
    this.junkDetectorProvider = junkDetectorProvider;
    this.duplicateDetectorProvider = duplicateDetectorProvider;
  }

  @Override
  public ImportContactsUseCase get() {
    return newInstance(contactImportParserProvider.get(), junkDetectorProvider.get(), duplicateDetectorProvider.get());
  }

  public static ImportContactsUseCase_Factory create(
      javax.inject.Provider<ContactImportParser> contactImportParserProvider,
      javax.inject.Provider<JunkDetector> junkDetectorProvider,
      javax.inject.Provider<DuplicateDetector> duplicateDetectorProvider) {
    return new ImportContactsUseCase_Factory(Providers.asDaggerProvider(contactImportParserProvider), Providers.asDaggerProvider(junkDetectorProvider), Providers.asDaggerProvider(duplicateDetectorProvider));
  }

  public static ImportContactsUseCase_Factory create(
      Provider<ContactImportParser> contactImportParserProvider,
      Provider<JunkDetector> junkDetectorProvider,
      Provider<DuplicateDetector> duplicateDetectorProvider) {
    return new ImportContactsUseCase_Factory(contactImportParserProvider, junkDetectorProvider, duplicateDetectorProvider);
  }

  public static ImportContactsUseCase newInstance(ContactImportParser contactImportParser,
      JunkDetector junkDetector, DuplicateDetector duplicateDetector) {
    return new ImportContactsUseCase(contactImportParser, junkDetector, duplicateDetector);
  }
}
