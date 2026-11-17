package com.ceoeg.aetherflow.core.vpn;

import com.ceoeg.aetherflow.core.common.logging.DiagnosticLogger;
import com.ceoeg.aetherflow.core.common.security.EncryptedSettingsStore;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.Provider;
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
public final class AetherVpnService_MembersInjector implements MembersInjector<AetherVpnService> {
  private final Provider<UnifiedEngineOrchestrator> orchestratorProvider;

  private final Provider<EncryptedSettingsStore> settingsStoreProvider;

  private final Provider<NetworkHandoffMonitor> handoffMonitorProvider;

  private final Provider<DiagnosticLogger> loggerProvider;

  private AetherVpnService_MembersInjector(Provider<UnifiedEngineOrchestrator> orchestratorProvider,
      Provider<EncryptedSettingsStore> settingsStoreProvider,
      Provider<NetworkHandoffMonitor> handoffMonitorProvider,
      Provider<DiagnosticLogger> loggerProvider) {
    this.orchestratorProvider = orchestratorProvider;
    this.settingsStoreProvider = settingsStoreProvider;
    this.handoffMonitorProvider = handoffMonitorProvider;
    this.loggerProvider = loggerProvider;
  }

  @Override
  public void injectMembers(AetherVpnService instance) {
    injectOrchestrator(instance, orchestratorProvider.get());
    injectSettingsStore(instance, settingsStoreProvider.get());
    injectHandoffMonitor(instance, handoffMonitorProvider.get());
    injectLogger(instance, loggerProvider.get());
  }

  public static MembersInjector<AetherVpnService> create(
      Provider<UnifiedEngineOrchestrator> orchestratorProvider,
      Provider<EncryptedSettingsStore> settingsStoreProvider,
      Provider<NetworkHandoffMonitor> handoffMonitorProvider,
      Provider<DiagnosticLogger> loggerProvider) {
    return new AetherVpnService_MembersInjector(orchestratorProvider, settingsStoreProvider, handoffMonitorProvider, loggerProvider);
  }

  @InjectedFieldSignature("com.ceoeg.aetherflow.core.vpn.AetherVpnService.orchestrator")
  public static void injectOrchestrator(AetherVpnService instance,
      UnifiedEngineOrchestrator orchestrator) {
    instance.orchestrator = orchestrator;
  }

  @InjectedFieldSignature("com.ceoeg.aetherflow.core.vpn.AetherVpnService.settingsStore")
  public static void injectSettingsStore(AetherVpnService instance,
      EncryptedSettingsStore settingsStore) {
    instance.settingsStore = settingsStore;
  }

  @InjectedFieldSignature("com.ceoeg.aetherflow.core.vpn.AetherVpnService.handoffMonitor")
  public static void injectHandoffMonitor(AetherVpnService instance,
      NetworkHandoffMonitor handoffMonitor) {
    instance.handoffMonitor = handoffMonitor;
  }

  @InjectedFieldSignature("com.ceoeg.aetherflow.core.vpn.AetherVpnService.logger")
  public static void injectLogger(AetherVpnService instance, DiagnosticLogger logger) {
    instance.logger = logger;
  }
}
