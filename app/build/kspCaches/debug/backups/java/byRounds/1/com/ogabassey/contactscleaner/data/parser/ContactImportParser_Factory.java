package com.ogabassey.contactscleaner.data.parser;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class ContactImportParser_Factory implements Factory<ContactImportParser> {
  @Override
  public ContactImportParser get() {
    return newInstance();
  }

  public static ContactImportParser_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ContactImportParser newInstance() {
    return new ContactImportParser();
  }

  private static final class InstanceHolder {
    static final ContactImportParser_Factory INSTANCE = new ContactImportParser_Factory();
  }
}
