package com.ogabassey.contactscleaner.di;

import com.ogabassey.contactscleaner.domain.repository.BillingRepository;
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
public final class DataModule_ProvideBillingRepositoryFactory implements Factory<BillingRepository> {
  @Override
  public BillingRepository get() {
    return provideBillingRepository();
  }

  public static DataModule_ProvideBillingRepositoryFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static BillingRepository provideBillingRepository() {
    return Preconditions.checkNotNullFromProvides(DataModule.INSTANCE.provideBillingRepository());
  }

  private static final class InstanceHolder {
    static final DataModule_ProvideBillingRepositoryFactory INSTANCE = new DataModule_ProvideBillingRepositoryFactory();
  }
}
