package com.ogabassey.contactscleaner.di;

import com.ogabassey.contactscleaner.data.db.ContactDatabase;
import com.ogabassey.contactscleaner.data.db.dao.UndoDao;
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
public final class DataModule_ProvideUndoDaoFactory implements Factory<UndoDao> {
  private final Provider<ContactDatabase> databaseProvider;

  public DataModule_ProvideUndoDaoFactory(Provider<ContactDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public UndoDao get() {
    return provideUndoDao(databaseProvider.get());
  }

  public static DataModule_ProvideUndoDaoFactory create(
      javax.inject.Provider<ContactDatabase> databaseProvider) {
    return new DataModule_ProvideUndoDaoFactory(Providers.asDaggerProvider(databaseProvider));
  }

  public static DataModule_ProvideUndoDaoFactory create(
      Provider<ContactDatabase> databaseProvider) {
    return new DataModule_ProvideUndoDaoFactory(databaseProvider);
  }

  public static UndoDao provideUndoDao(ContactDatabase database) {
    return Preconditions.checkNotNullFromProvides(DataModule.INSTANCE.provideUndoDao(database));
  }
}
