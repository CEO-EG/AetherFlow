package com.ceoeg.aetherflow.feature.dashboard;

import com.ceoeg.aetherflow.core.common.database.TunnelRepository;
import com.ceoeg.aetherflow.core.common.security.EncryptedSettingsStore;
import com.ceoeg.aetherflow.core.vpn.UnifiedEngineOrchestrator;
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
public final class DashboardViewModel_Factory implements Factory<DashboardViewModel> {
  private final Provider<UnifiedEngineOrchestrator> orchestratorProvider;

  private final Provider<TunnelRepository> repositoryProvider;

  private final Provider<EncryptedSettingsStore> settingsStoreProvider;

  private DashboardViewModel_Factory(Provider<UnifiedEngineOrchestrator> orchestratorProvider,
      Provider<TunnelRepository> repositoryProvider,
      Provider<EncryptedSettingsStore> settingsStoreProvider) {
    this.orchestratorProvider = orchestratorProvider;
    this.repositoryProvider = repositoryProvider;
    this.settingsStoreProvider = settingsStoreProvider;
  }

  @Override
  public DashboardViewModel get() {
    return newInstance(orchestratorProvider.get(), repositoryProvider.get(), settingsStoreProvider.get());
  }

  public static DashboardViewModel_Factory create(
      Provider<UnifiedEngineOrchestrator> orchestratorProvider,
      Provider<TunnelRepository> repositoryProvider,
      Provider<EncryptedSettingsStore> settingsStoreProvider) {
    return new DashboardViewModel_Factory(orchestratorProvider, repositoryProvider, settingsStoreProvider);
  }

  public static DashboardViewModel newInstance(UnifiedEngineOrchestrator orchestrator,
      TunnelRepository repository, EncryptedSettingsStore settingsStore) {
    return new DashboardViewModel(orchestrator, repository, settingsStore);
  }
}
