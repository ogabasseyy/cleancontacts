package com.ogabassey.contactscleaner;

import androidx.hilt.work.HiltWorkerFactory;
import com.ogabassey.contactscleaner.domain.repository.BillingRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;

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
public final class CleanContactsApp_MembersInjector implements MembersInjector<CleanContactsApp> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  private final Provider<BillingRepository> billingRepositoryProvider;

  public CleanContactsApp_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider,
      Provider<BillingRepository> billingRepositoryProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
    this.billingRepositoryProvider = billingRepositoryProvider;
  }

  public static MembersInjector<CleanContactsApp> create(
      Provider<HiltWorkerFactory> workerFactoryProvider,
      Provider<BillingRepository> billingRepositoryProvider) {
    return new CleanContactsApp_MembersInjector(workerFactoryProvider, billingRepositoryProvider);
  }

  public static MembersInjector<CleanContactsApp> create(
      javax.inject.Provider<HiltWorkerFactory> workerFactoryProvider,
      javax.inject.Provider<BillingRepository> billingRepositoryProvider) {
    return new CleanContactsApp_MembersInjector(Providers.asDaggerProvider(workerFactoryProvider), Providers.asDaggerProvider(billingRepositoryProvider));
  }

  @Override
  public void injectMembers(CleanContactsApp instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
    injectBillingRepository(instance, billingRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.ogabassey.contactscleaner.CleanContactsApp.workerFactory")
  public static void injectWorkerFactory(CleanContactsApp instance,
      HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }

  @InjectedFieldSignature("com.ogabassey.contactscleaner.CleanContactsApp.billingRepository")
  public static void injectBillingRepository(CleanContactsApp instance,
      BillingRepository billingRepository) {
    instance.billingRepository = billingRepository;
  }
}
