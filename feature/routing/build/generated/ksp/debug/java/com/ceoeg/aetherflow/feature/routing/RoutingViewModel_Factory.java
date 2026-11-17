package com.ceoeg.aetherflow.feature.routing;

import com.ceoeg.aetherflow.core.common.database.TunnelRepository;
import com.ceoeg.aetherflow.core.common.logging.DiagnosticLogger;
import com.ceoeg.aetherflow.core.common.security.EncryptedSettingsStore;
import com.ceoeg.aetherflow.core.routing.GeoRuleSetManager;
import com.ceoeg.aetherflow.core.routing.SplitTunnelAppManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
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
public final class RoutingViewModel_Factory implements Factory<RoutingViewModel> {
  private final Provider<EncryptedSettingsStore> settingsStoreProvider;

  private final Provider<TunnelRepository> repositoryProvider;

  private final Provider<SplitTunnelAppManager> appManagerProvider;

  private final Provider<GeoRuleSetManager> geoManagerProvider;

  private final Provider<DiagnosticLogger> loggerProvider;

  private RoutingViewModel_Factory(Provider<EncryptedSettingsStore> settingsStoreProvider,
      Provider<TunnelRepository> repositoryProvider,
      Provider<SplitTunnelAppManager> appManagerProvider,
      Provider<GeoRuleSetManager> geoManagerProvider, Provider<DiagnosticLogger> loggerProvider) {
    this.settingsStoreProvider = settingsStoreProvider;
    this.repositoryProvider = repositoryProvider;
    this.appManagerProvider = appManagerProvider;
    this.geoManagerProvider = geoManagerProvider;
    this.loggerProvider = loggerProvider;
  }

  @Override
  public RoutingViewModel get() {
    return newInstance(settingsStoreProvider.get(), repositoryProvider.get(), appManagerProvider.get(), geoManagerProvider.get(), loggerProvider.get());
  }

  public static RoutingViewModel_Factory create(
      Provider<EncryptedSettingsStore> settingsStoreProvider,
      Provider<TunnelRepository> repositoryProvider,
      Provider<SplitTunnelAppManager> appManagerProvider,
      Provider<GeoRuleSetManager> geoManagerProvider, Provider<DiagnosticLogger> loggerProvider) {
    return new RoutingViewModel_Factory(settingsStoreProvider, repositoryProvider, appManagerProvider, geoManagerProvider, loggerProvider);
  }

  public static RoutingViewModel newInstance(EncryptedSettingsStore settingsStore,
      TunnelRepository repository, SplitTunnelAppManager appManager, GeoRuleSetManager geoManager,
      DiagnosticLogger logger) {
    return new RoutingViewModel(settingsStore, repository, appManager, geoManager, logger);
  }
}
