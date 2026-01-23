package com.ogabassey.contactscleaner.di;

import com.ogabassey.contactscleaner.data.db.ContactDatabase;
import com.ogabassey.contactscleaner.data.db.dao.ContactDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DataModule_ProvideContactDaoFactory implements Factory<ContactDao> {
  private final Provider<ContactDatabase> databaseProvider;

  public DataModule_ProvideContactDaoFactory(Provider<ContactDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public ContactDao get() {
    return provideContactDao(databaseProvider.get());
  }

  public static DataModule_ProvideContactDaoFactory create(
      javax.inject.Provider<ContactDatabase> databaseProvider) {
    return new DataModule_ProvideContactDaoFactory(Providers.asDaggerProvider(databaseProvider));
  }

  public static DataModule_ProvideContactDaoFactory create(
      Provider<ContactDatabase> databaseProvider) {
    return new DataModule_ProvideContactDaoFactory(databaseProvider);
  }

  public static ContactDao provideContactDao(ContactDatabase database) {
    return Preconditions.checkNotNullFromProvides(DataModule.INSTANCE.provideContactDao(database));
  }
}
