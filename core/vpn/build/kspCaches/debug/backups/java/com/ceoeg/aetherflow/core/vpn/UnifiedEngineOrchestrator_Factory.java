package com.ceoeg.aetherflow.core.vpn;

import android.content.Context;
import com.ceoeg.aetherflow.core.common.database.TunnelRepository;
import com.ceoeg.aetherflow.core.common.logging.DiagnosticLogger;
import com.ceoeg.aetherflow.core.common.security.EncryptedSettingsStore;
import com.ceoeg.aetherflow.core.common.security.EphemeralLoopbackAuthGenerator;
import com.ceoeg.aetherflow.core.singbox.ClashApiInspectorClient;
import com.ceoeg.aetherflow.core.singbox.SingBoxConfigBuilder;
import com.ceoeg.aetherflow.core.singbox.SingBoxEngineController;
import com.ceoeg.aetherflow.core.tun.TunEngineController;
import com.ceoeg.aetherflow.core.wireguard.WireGuardEngineController;
import com.ceoeg.aetherflow.core.xray.XrayConfigBuilder;
import com.ceoeg.aetherflow.core.xray.XrayEngineController;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class UnifiedEngineOrchestrator_Factory implements Factory<UnifiedEngineOrchestrator> {
  private final Provider<Context> appContextProvider;

  private final Provider<VpnStateMachine> stateMachineProvider;

  private final Provider<TunnelRepository> repositoryProvider;

  private final Provider<EncryptedSettingsStore> settingsStoreProvider;

  private final Provider<EphemeralLoopbackAuthGenerator> authGeneratorProvider;

  private final Provider<XrayConfigBuilder> xrayConfigBuilderProvider;

  private final Provider<XrayEngineController> xrayControllerProvider;

  private final Provider<SingBoxConfigBuilder> singBoxConfigBuilderProvider;

  private final Provider<SingBoxEngineController> singBoxControllerProvider;

  private final Provider<ClashApiInspectorClient> clashInspectorProvider;

  private final Provider<WireGuardEngineController> wireGuardControllerProvider;

  private final Provider<TunEngineController> tunControllerProvider;

  private final Provider<SmartNodeSelector> smartNodeSelectorProvider;

  private final Provider<EndpointProber> proberProvider;

  private final Provider<ExitNetworkVerifier> exitVerifierProvider;

  private final Provider<DiagnosticLogger> loggerProvider;

  private UnifiedEngineOrchestrator_Factory(Provider<Context> appContextProvider,
      Provider<VpnStateMachine> stateMachineProvider, Provider<TunnelRepository> repositoryProvider,
      Provider<EncryptedSettingsStore> settingsStoreProvider,
      Provider<EphemeralLoopbackAuthGenerator> authGeneratorProvider,
      Provider<XrayConfigBuilder> xrayConfigBuilderProvider,
      Provider<XrayEngineController> xrayControllerProvider,
      Provider<SingBoxConfigBuilder> singBoxConfigBuilderProvider,
      Provider<SingBoxEngineController> singBoxControllerProvider,
      Provider<ClashApiInspectorClient> clashInspectorProvider,
      Provider<WireGuardEngineController> wireGuardControllerProvider,
      Provider<TunEngineController> tunControllerProvider,
      Provider<SmartNodeSelector> smartNodeSelectorProvider,
      Provider<EndpointProber> proberProvider, Provider<ExitNetworkVerifier> exitVerifierProvider,
      Provider<DiagnosticLogger> loggerProvider) {
    this.appContextProvider = appContextProvider;
    this.stateMachineProvider = stateMachineProvider;
    this.repositoryProvider = repositoryProvider;
    this.settingsStoreProvider = settingsStoreProvider;
    this.authGeneratorProvider = authGeneratorProvider;
    this.xrayConfigBuilderProvider = xrayConfigBuilderProvider;
    this.xrayControllerProvider = xrayControllerProvider;
    this.singBoxConfigBuilderProvider = singBoxConfigBuilderProvider;
    this.singBoxControllerProvider = singBoxControllerProvider;
    this.clashInspectorProvider = clashInspectorProvider;
    this.wireGuardControllerProvider = wireGuardControllerProvider;
    this.tunControllerProvider = tunControllerProvider;
    this.smartNodeSelectorProvider = smartNodeSelectorProvider;
    this.proberProvider = proberProvider;
    this.exitVerifierProvider = exitVerifierProvider;
    this.loggerProvider = loggerProvider;
  }

  @Override
  public UnifiedEngineOrchestrator get() {
    return newInstance(appContextProvider.get(), stateMachineProvider.get(), repositoryProvider.get(), settingsStoreProvider.get(), authGeneratorProvider.get(), xrayConfigBuilderProvider.get(), xrayControllerProvider.get(), singBoxConfigBuilderProvider.get(), singBoxControllerProvider.get(), clashInspectorProvider.get(), wireGuardControllerProvider.get(), tunControllerProvider.get(), smartNodeSelectorProvider.get(), proberProvider.get(), exitVerifierProvider.get(), loggerProvider.get());
  }

  public static UnifiedEngineOrchestrator_Factory create(Provider<Context> appContextProvider,
      Provider<VpnStateMachine> stateMachineProvider, Provider<TunnelRepository> repositoryProvider,
      Provider<EncryptedSettingsStore> settingsStoreProvider,
      Provider<EphemeralLoopbackAuthGenerator> authGeneratorProvider,
      Provider<XrayConfigBuilder> xrayConfigBuilderProvider,
      Provider<XrayEngineController> xrayControllerProvider,
      Provider<SingBoxConfigBuilder> singBoxConfigBuilderProvider,
      Provider<SingBoxEngineController> singBoxControllerProvider,
      Provider<ClashApiInspectorClient> clashInspectorProvider,
      Provider<WireGuardEngineController> wireGuardControllerProvider,
      Provider<TunEngineController> tunControllerProvider,
      Provider<SmartNodeSelector> smartNodeSelectorProvider,
      Provider<EndpointProber> proberProvider, Provider<ExitNetworkVerifier> exitVerifierProvider,
      Provider<DiagnosticLogger> loggerProvider) {
    return new UnifiedEngineOrchestrator_Factory(appContextProvider, stateMachineProvider, repositoryProvider, settingsStoreProvider, authGeneratorProvider, xrayConfigBuilderProvider, xrayControllerProvider, singBoxConfigBuilderProvider, singBoxControllerProvider, clashInspectorProvider, wireGuardControllerProvider, tunControllerProvider, smartNodeSelectorProvider, proberProvider, exitVerifierProvider, loggerProvider);
  }

  public static UnifiedEngineOrchestrator newInstance(Context appContext,
      VpnStateMachine stateMachine, TunnelRepository repository,
      EncryptedSettingsStore settingsStore, EphemeralLoopbackAuthGenerator authGenerator,
      XrayConfigBuilder xrayConfigBuilder, XrayEngineController xrayController,
      SingBoxConfigBuilder singBoxConfigBuilder, SingBoxEngineController singBoxController,
      ClashApiInspectorClient clashInspector, WireGuardEngineController wireGuardController,
      TunEngineController tunController, SmartNodeSelector smartNodeSelector, EndpointProber prober,
      ExitNetworkVerifier exitVerifier, DiagnosticLogger logger) {
    return new UnifiedEngineOrchestrator(appContext, stateMachine, repository, settingsStore, authGenerator, xrayConfigBuilder, xrayController, singBoxConfigBuilder, singBoxController, clashInspector, wireGuardController, tunController, smartNodeSelector, prober, exitVerifier, logger);
  }
}
