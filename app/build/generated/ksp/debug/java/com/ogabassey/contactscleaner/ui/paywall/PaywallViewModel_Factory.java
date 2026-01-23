package com.ogabassey.contactscleaner.ui.paywall;

import com.ogabassey.contactscleaner.domain.repository.BillingRepository;
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
public final class PaywallViewModel_Factory implements Factory<PaywallViewModel> {
  private final Provider<BillingRepository> billingRepositoryProvider;

  public PaywallViewModel_Factory(Provider<BillingRepository> billingRepositoryProvider) {
    this.billingRepositoryProvider = billingRepositoryProvider;
  }

  @Override
  public PaywallViewModel get() {
    return newInstance(billingRepositoryProvider.get());
  }

  public static PaywallViewModel_Factory create(
      javax.inject.Provider<BillingRepository> billingRepositoryProvider) {
    return new PaywallViewModel_Factory(Providers.asDaggerProvider(billingRepositoryProvider));
  }

  public static PaywallViewModel_Factory create(
      Provider<BillingRepository> billingRepositoryProvider) {
    return new PaywallViewModel_Factory(billingRepositoryProvider);
  }

  public static PaywallViewModel newInstance(BillingRepository billingRepository) {
    return new PaywallViewModel(billingRepository);
  }
}
