package com.ogabassey.contactscleaner.data.repository;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class RevenueCatBillingRepository_Factory implements Factory<RevenueCatBillingRepository> {
  @Override
  public RevenueCatBillingRepository get() {
    return newInstance();
  }

  public static RevenueCatBillingRepository_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static RevenueCatBillingRepository newInstance() {
    return new RevenueCatBillingRepository();
  }

  private static final class InstanceHolder {
    static final RevenueCatBillingRepository_Factory INSTANCE = new RevenueCatBillingRepository_Factory();
  }
}
