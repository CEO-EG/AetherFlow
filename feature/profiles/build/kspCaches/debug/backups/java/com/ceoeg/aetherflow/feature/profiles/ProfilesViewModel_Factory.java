package com.ceoeg.aetherflow.feature.profiles;

import com.ceoeg.aetherflow.core.common.database.TunnelRepository;
import com.ceoeg.aetherflow.core.common.security.EncryptedSettingsStore;
import com.ceoeg.aetherflow.core.parser.UniversalConfigParser;
import com.ceoeg.aetherflow.core.vpn.SmartNodeSelector;
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
public final class ProfilesViewModel_Factory implements Factory<ProfilesViewModel> {
  private final Provider<TunnelRepository> repositoryProvider;

  private final Provider<EncryptedSettingsStore> settingsStoreProvider;

  private final Provider<UniversalConfigParser> parserProvider;

  private final Provider<SmartNodeSelector> smartSelectorProvider;

  private final Provider<UnifiedEngineOrchestrator> orchestratorProvider;

  private ProfilesViewModel_Factory(Provider<TunnelRepository> repositoryProvider,
      Provider<EncryptedSettingsStore> settingsStoreProvider,
      Provider<UniversalConfigParser> parserProvider,
      Provider<SmartNodeSelector> smartSelectorProvider,
      Provider<UnifiedEngineOrchestrator> orchestratorProvider) {
    this.repositoryProvider = repositoryProvider;
    this.settingsStoreProvider = settingsStoreProvider;
    this.parserProvider = parserProvider;
    this.smartSelectorProvider = smartSelectorProvider;
    this.orchestratorProvider = orchestratorProvider;
  }

  @Override
  public ProfilesViewModel get() {
    return newInstance(repositoryProvider.get(), settingsStoreProvider.get(), parserProvider.get(), smartSelectorProvider.get(), orchestratorProvider.get());
  }

  public static ProfilesViewModel_Factory create(Provider<TunnelRepository> repositoryProvider,
      Provider<EncryptedSettingsStore> settingsStoreProvider,
      Provider<UniversalConfigParser> parserProvider,
      Provider<SmartNodeSelector> smartSelectorProvider,
      Provider<UnifiedEngineOrchestrator> orchestratorProvider) {
    return new ProfilesViewModel_Factory(repositoryProvider, settingsStoreProvider, parserProvider, smartSelectorProvider, orchestratorProvider);
  }

  public static ProfilesViewModel newInstance(TunnelRepository repository,
      EncryptedSettingsStore settingsStore, UniversalConfigParser parser,
      SmartNodeSelector smartSelector, UnifiedEngineOrchestrator orchestrator) {
    return new ProfilesViewModel(repository, settingsStore, parser, smartSelector, orchestrator);
  }
}
