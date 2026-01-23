package com.ogabassey.contactscleaner.di;

import com.ogabassey.contactscleaner.data.parser.ContactImportParser;
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
public final class DataModule_ProvideContactImportParserFactory implements Factory<ContactImportParser> {
  @Override
  public ContactImportParser get() {
    return provideContactImportParser();
  }

  public static DataModule_ProvideContactImportParserFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ContactImportParser provideContactImportParser() {
    return Preconditions.checkNotNullFromProvides(DataModule.INSTANCE.provideContactImportParser());
  }

  private static final class InstanceHolder {
    static final DataModule_ProvideContactImportParserFactory INSTANCE = new DataModule_ProvideContactImportParserFactory();
  }
}
