package com.ceoeg.aetherflow.feature.diagnostics;

import com.ceoeg.aetherflow.core.common.logging.DiagnosticLogger;
import com.ceoeg.aetherflow.core.common.security.EncryptedSettingsStore;
import com.ceoeg.aetherflow.core.singbox.ClashApiInspectorClient;
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
public final class DiagnosticsViewModel_Factory implements Factory<DiagnosticsViewModel> {
  private final Provider<UnifiedEngineOrchestrator> orchestratorProvider;

  private final Provider<ClashApiInspectorClient> clashInspectorProvider;

  private final Provider<DiagnosticLogger> loggerProvider;

  private final Provider<EncryptedSettingsStore> settingsStoreProvider;

  private DiagnosticsViewModel_Factory(Provider<UnifiedEngineOrchestrator> orchestratorProvider,
      Provider<ClashApiInspectorClient> clashInspectorProvider,
      Provider<DiagnosticLogger> loggerProvider,
      Provider<EncryptedSettingsStore> settingsStoreProvider) {
    this.orchestratorProvider = orchestratorProvider;
    this.clashInspectorProvider = clashInspectorProvider;
    this.loggerProvider = loggerProvider;
    this.settingsStoreProvider = settingsStoreProvider;
  }

  @Override
  public DiagnosticsViewModel get() {
    return newInstance(orchestratorProvider.get(), clashInspectorProvider.get(), loggerProvider.get(), settingsStoreProvider.get());
  }

  public static DiagnosticsViewModel_Factory create(
      Provider<UnifiedEngineOrchestrator> orchestratorProvider,
      Provider<ClashApiInspectorClient> clashInspectorProvider,
      Provider<DiagnosticLogger> loggerProvider,
      Provider<EncryptedSettingsStore> settingsStoreProvider) {
    return new DiagnosticsViewModel_Factory(orchestratorProvider, clashInspectorProvider, loggerProvider, settingsStoreProvider);
  }

  public static DiagnosticsViewModel newInstance(UnifiedEngineOrchestrator orchestrator,
      ClashApiInspectorClient clashInspector, DiagnosticLogger logger,
      EncryptedSettingsStore settingsStore) {
    return new DiagnosticsViewModel(orchestrator, clashInspector, logger, settingsStore);
  }
}
