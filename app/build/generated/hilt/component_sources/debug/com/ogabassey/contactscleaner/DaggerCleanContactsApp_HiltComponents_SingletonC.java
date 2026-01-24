package com.ogabassey.contactscleaner;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.hilt.work.WorkerAssistedFactory;
import androidx.hilt.work.WorkerFactoryModule_ProvideFactoryFactory;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import androidx.work.ListenableWorker;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.ogabassey.contactscleaner.data.db.ContactDatabase;
import com.ogabassey.contactscleaner.data.db.dao.ContactDao;
import com.ogabassey.contactscleaner.data.db.dao.IgnoredContactDao;
import com.ogabassey.contactscleaner.data.db.dao.UndoDao;
import com.ogabassey.contactscleaner.data.detector.DuplicateDetector;
import com.ogabassey.contactscleaner.data.detector.FormatDetector;
import com.ogabassey.contactscleaner.data.detector.JunkDetector;
import com.ogabassey.contactscleaner.data.detector.SensitiveDataDetector;
import com.ogabassey.contactscleaner.data.provider.RegionProvider;
import com.ogabassey.contactscleaner.data.repository.ScanSettingsRepository;
import com.ogabassey.contactscleaner.data.repository.UsageRepository;
import com.ogabassey.contactscleaner.data.source.ContactsProviderSource;
import com.ogabassey.contactscleaner.data.util.ScanResultProvider;
import com.ogabassey.contactscleaner.di.DataModule_ProvideBackupRepositoryFactory;
import com.ogabassey.contactscleaner.di.DataModule_ProvideBillingRepositoryFactory;
import com.ogabassey.contactscleaner.di.DataModule_ProvideContactDaoFactory;
import com.ogabassey.contactscleaner.di.DataModule_ProvideContactDatabaseFactory;
import com.ogabassey.contactscleaner.di.DataModule_ProvideContactRepositoryFactory;
import com.ogabassey.contactscleaner.di.DataModule_ProvideContactsProviderSourceFactory;
import com.ogabassey.contactscleaner.di.DataModule_ProvideDuplicateDetectorFactory;
import com.ogabassey.contactscleaner.di.DataModule_ProvideFileServiceFactory;
import com.ogabassey.contactscleaner.di.DataModule_ProvideIgnoredContactDaoFactory;
import com.ogabassey.contactscleaner.di.DataModule_ProvideJunkDetectorFactory;
import com.ogabassey.contactscleaner.di.DataModule_ProvideRegionProviderFactory;
import com.ogabassey.contactscleaner.di.DataModule_ProvideUndoDaoFactory;
import com.ogabassey.contactscleaner.domain.repository.BackupRepository;
import com.ogabassey.contactscleaner.domain.repository.BillingRepository;
import com.ogabassey.contactscleaner.domain.repository.ContactRepository;
import com.ogabassey.contactscleaner.domain.repository.FileService;
import com.ogabassey.contactscleaner.domain.usecase.CleanupContactsUseCase;
import com.ogabassey.contactscleaner.domain.usecase.ExportUseCase;
import com.ogabassey.contactscleaner.domain.usecase.GetContactsPagedUseCase;
import com.ogabassey.contactscleaner.domain.usecase.ScanContactsUseCase;
import com.ogabassey.contactscleaner.domain.usecase.UndoUseCase;
import com.ogabassey.contactscleaner.ui.dashboard.DashboardViewModel;
import com.ogabassey.contactscleaner.ui.dashboard.DashboardViewModel_HiltModules;
import com.ogabassey.contactscleaner.ui.dashboard.DashboardViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.ogabassey.contactscleaner.ui.dashboard.DashboardViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.ogabassey.contactscleaner.ui.history.RecentActionsViewModel;
import com.ogabassey.contactscleaner.ui.history.RecentActionsViewModel_HiltModules;
import com.ogabassey.contactscleaner.ui.history.RecentActionsViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.ogabassey.contactscleaner.ui.history.RecentActionsViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.ogabassey.contactscleaner.ui.paywall.PaywallViewModel;
import com.ogabassey.contactscleaner.ui.paywall.PaywallViewModel_HiltModules;
import com.ogabassey.contactscleaner.ui.paywall.PaywallViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.ogabassey.contactscleaner.ui.paywall.PaywallViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.ogabassey.contactscleaner.ui.results.ResultsViewModel;
import com.ogabassey.contactscleaner.ui.results.ResultsViewModel_HiltModules;
import com.ogabassey.contactscleaner.ui.results.ResultsViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.ogabassey.contactscleaner.ui.results.ResultsViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.ogabassey.contactscleaner.ui.tools.ExportViewModel;
import com.ogabassey.contactscleaner.ui.tools.ExportViewModel_HiltModules;
import com.ogabassey.contactscleaner.ui.tools.ExportViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.ogabassey.contactscleaner.ui.tools.ExportViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

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
public final class DaggerCleanContactsApp_HiltComponents_SingletonC {
  private DaggerCleanContactsApp_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public CleanContactsApp_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements CleanContactsApp_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public CleanContactsApp_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements CleanContactsApp_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public CleanContactsApp_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements CleanContactsApp_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public CleanContactsApp_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements CleanContactsApp_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public CleanContactsApp_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements CleanContactsApp_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public CleanContactsApp_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements CleanContactsApp_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public CleanContactsApp_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements CleanContactsApp_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public CleanContactsApp_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends CleanContactsApp_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends CleanContactsApp_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends CleanContactsApp_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends CleanContactsApp_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(ImmutableMap.<String, Boolean>of(DashboardViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, DashboardViewModel_HiltModules.KeyModule.provide(), ExportViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ExportViewModel_HiltModules.KeyModule.provide(), PaywallViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, PaywallViewModel_HiltModules.KeyModule.provide(), RecentActionsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, RecentActionsViewModel_HiltModules.KeyModule.provide(), ResultsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ResultsViewModel_HiltModules.KeyModule.provide()));
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }
  }

  private static final class ViewModelCImpl extends CleanContactsApp_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<DashboardViewModel> dashboardViewModelProvider;

    private Provider<ExportViewModel> exportViewModelProvider;

    private Provider<PaywallViewModel> paywallViewModelProvider;

    private Provider<RecentActionsViewModel> recentActionsViewModelProvider;

    private Provider<ResultsViewModel> resultsViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    private ScanContactsUseCase scanContactsUseCase() {
      return new ScanContactsUseCase(singletonCImpl.provideContactRepositoryProvider.get());
    }

    private ExportUseCase exportUseCase() {
      return new ExportUseCase(singletonCImpl.provideContactRepositoryProvider.get(), singletonCImpl.provideFileServiceProvider.get());
    }

    private GetContactsPagedUseCase getContactsPagedUseCase() {
      return new GetContactsPagedUseCase(singletonCImpl.provideContactRepositoryProvider.get());
    }

    private CleanupContactsUseCase cleanupContactsUseCase() {
      return new CleanupContactsUseCase(singletonCImpl.provideContactRepositoryProvider.get(), singletonCImpl.provideBackupRepositoryProvider.get());
    }

    private UndoUseCase undoUseCase() {
      return new UndoUseCase(singletonCImpl.provideBackupRepositoryProvider.get(), singletonCImpl.provideContactRepositoryProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.dashboardViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.exportViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.paywallViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.recentActionsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.resultsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(ImmutableMap.<String, javax.inject.Provider<ViewModel>>of(DashboardViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) dashboardViewModelProvider), ExportViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) exportViewModelProvider), PaywallViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) paywallViewModelProvider), RecentActionsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) recentActionsViewModelProvider), ResultsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) resultsViewModelProvider)));
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return ImmutableMap.<Class<?>, Object>of();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.ogabassey.contactscleaner.ui.dashboard.DashboardViewModel 
          return (T) new DashboardViewModel(viewModelCImpl.scanContactsUseCase(), singletonCImpl.scanResultProvider.get());

          case 1: // com.ogabassey.contactscleaner.ui.tools.ExportViewModel 
          return (T) new ExportViewModel(viewModelCImpl.exportUseCase());

          case 2: // com.ogabassey.contactscleaner.ui.paywall.PaywallViewModel 
          return (T) new PaywallViewModel(singletonCImpl.provideBillingRepositoryProvider.get());

          case 3: // com.ogabassey.contactscleaner.ui.history.RecentActionsViewModel 
          return (T) new RecentActionsViewModel(singletonCImpl.provideBackupRepositoryProvider.get(), singletonCImpl.provideContactRepositoryProvider.get());

          case 4: // com.ogabassey.contactscleaner.ui.results.ResultsViewModel 
          return (T) new ResultsViewModel(viewModelCImpl.getContactsPagedUseCase(), viewModelCImpl.cleanupContactsUseCase(), singletonCImpl.scanResultProvider.get(), singletonCImpl.provideContactRepositoryProvider.get(), singletonCImpl.provideBillingRepositoryProvider.get(), singletonCImpl.formatDetectorProvider.get(), viewModelCImpl.exportUseCase(), viewModelCImpl.undoUseCase(), singletonCImpl.usageRepositoryProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends CleanContactsApp_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends CleanContactsApp_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends CleanContactsApp_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<BillingRepository> provideBillingRepositoryProvider;

    private Provider<ContactDatabase> provideContactDatabaseProvider;

    private Provider<ContactsProviderSource> provideContactsProviderSourceProvider;

    private Provider<JunkDetector> provideJunkDetectorProvider;

    private Provider<RegionProvider> provideRegionProvider;

    private Provider<DuplicateDetector> provideDuplicateDetectorProvider;

    private Provider<FormatDetector> formatDetectorProvider;

    private Provider<SensitiveDataDetector> sensitiveDataDetectorProvider;

    private Provider<ScanSettingsRepository> scanSettingsRepositoryProvider;

    private Provider<ScanResultProvider> scanResultProvider;

    private Provider<ContactRepository> provideContactRepositoryProvider;

    private Provider<FileService> provideFileServiceProvider;

    private Provider<BackupRepository> provideBackupRepositoryProvider;

    private Provider<UsageRepository> usageRepositoryProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    private HiltWorkerFactory hiltWorkerFactory() {
      return WorkerFactoryModule_ProvideFactoryFactory.provideFactory(ImmutableMap.<String, javax.inject.Provider<WorkerAssistedFactory<? extends ListenableWorker>>>of());
    }

    private ContactDao contactDao() {
      return DataModule_ProvideContactDaoFactory.provideContactDao(provideContactDatabaseProvider.get());
    }

    private IgnoredContactDao ignoredContactDao() {
      return DataModule_ProvideIgnoredContactDaoFactory.provideIgnoredContactDao(provideContactDatabaseProvider.get());
    }

    private UndoDao undoDao() {
      return DataModule_ProvideUndoDaoFactory.provideUndoDao(provideContactDatabaseProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideBillingRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<BillingRepository>(singletonCImpl, 0));
      this.provideContactDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<ContactDatabase>(singletonCImpl, 2));
      this.provideContactsProviderSourceProvider = DoubleCheck.provider(new SwitchingProvider<ContactsProviderSource>(singletonCImpl, 3));
      this.provideJunkDetectorProvider = DoubleCheck.provider(new SwitchingProvider<JunkDetector>(singletonCImpl, 4));
      this.provideRegionProvider = DoubleCheck.provider(new SwitchingProvider<RegionProvider>(singletonCImpl, 6));
      this.provideDuplicateDetectorProvider = DoubleCheck.provider(new SwitchingProvider<DuplicateDetector>(singletonCImpl, 5));
      this.formatDetectorProvider = DoubleCheck.provider(new SwitchingProvider<FormatDetector>(singletonCImpl, 7));
      this.sensitiveDataDetectorProvider = DoubleCheck.provider(new SwitchingProvider<SensitiveDataDetector>(singletonCImpl, 8));
      this.scanSettingsRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<ScanSettingsRepository>(singletonCImpl, 10));
      this.scanResultProvider = DoubleCheck.provider(new SwitchingProvider<ScanResultProvider>(singletonCImpl, 9));
      this.provideContactRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<ContactRepository>(singletonCImpl, 1));
      this.provideFileServiceProvider = DoubleCheck.provider(new SwitchingProvider<FileService>(singletonCImpl, 11));
      this.provideBackupRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<BackupRepository>(singletonCImpl, 12));
      this.usageRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<UsageRepository>(singletonCImpl, 13));
    }

    @Override
    public void injectCleanContactsApp(CleanContactsApp cleanContactsApp) {
      injectCleanContactsApp2(cleanContactsApp);
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return ImmutableSet.<Boolean>of();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    @CanIgnoreReturnValue
    private CleanContactsApp injectCleanContactsApp2(CleanContactsApp instance) {
      CleanContactsApp_MembersInjector.injectWorkerFactory(instance, hiltWorkerFactory());
      CleanContactsApp_MembersInjector.injectBillingRepository(instance, provideBillingRepositoryProvider.get());
      return instance;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.ogabassey.contactscleaner.domain.repository.BillingRepository 
          return (T) DataModule_ProvideBillingRepositoryFactory.provideBillingRepository();

          case 1: // com.ogabassey.contactscleaner.domain.repository.ContactRepository 
          return (T) DataModule_ProvideContactRepositoryFactory.provideContactRepository(singletonCImpl.contactDao(), singletonCImpl.provideContactsProviderSourceProvider.get(), singletonCImpl.provideJunkDetectorProvider.get(), singletonCImpl.provideDuplicateDetectorProvider.get(), singletonCImpl.formatDetectorProvider.get(), singletonCImpl.sensitiveDataDetectorProvider.get(), singletonCImpl.ignoredContactDao(), singletonCImpl.scanResultProvider.get());

          case 2: // com.ogabassey.contactscleaner.data.db.ContactDatabase 
          return (T) DataModule_ProvideContactDatabaseFactory.provideContactDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 3: // com.ogabassey.contactscleaner.data.source.ContactsProviderSource 
          return (T) DataModule_ProvideContactsProviderSourceFactory.provideContactsProviderSource(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 4: // com.ogabassey.contactscleaner.data.detector.JunkDetector 
          return (T) DataModule_ProvideJunkDetectorFactory.provideJunkDetector();

          case 5: // com.ogabassey.contactscleaner.data.detector.DuplicateDetector 
          return (T) DataModule_ProvideDuplicateDetectorFactory.provideDuplicateDetector(singletonCImpl.provideRegionProvider.get());

          case 6: // com.ogabassey.contactscleaner.data.provider.RegionProvider 
          return (T) DataModule_ProvideRegionProviderFactory.provideRegionProvider(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 7: // com.ogabassey.contactscleaner.data.detector.FormatDetector 
          return (T) new FormatDetector();

          case 8: // com.ogabassey.contactscleaner.data.detector.SensitiveDataDetector 
          return (T) new SensitiveDataDetector();

          case 9: // com.ogabassey.contactscleaner.data.util.ScanResultProvider 
          return (T) new ScanResultProvider(singletonCImpl.scanSettingsRepositoryProvider.get());

          case 10: // com.ogabassey.contactscleaner.data.repository.ScanSettingsRepository 
          return (T) new ScanSettingsRepository(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 11: // com.ogabassey.contactscleaner.domain.repository.FileService 
          return (T) DataModule_ProvideFileServiceFactory.provideFileService(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 12: // com.ogabassey.contactscleaner.domain.repository.BackupRepository 
          return (T) DataModule_ProvideBackupRepositoryFactory.provideBackupRepository(singletonCImpl.undoDao());

          case 13: // com.ogabassey.contactscleaner.data.repository.UsageRepository 
          return (T) new UsageRepository(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
