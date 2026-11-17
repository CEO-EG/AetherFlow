package com.ceoeg.aetherflow.core.wireguard;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
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
public final class WireGuardEngineController_Factory implements Factory<WireGuardEngineController> {
  private final Provider<AmneziaWireGuardConfigBuilder> configBuilderProvider;

  private final Provider<WireGuardNativeBridge> bridgeProvider;

  private WireGuardEngineController_Factory(
      Provider<AmneziaWireGuardConfigBuilder> configBuilderProvider,
      Provider<WireGuardNativeBridge> bridgeProvider) {
    this.configBuilderProvider = configBuilderProvider;
    this.bridgeProvider = bridgeProvider;
  }

  @Override
  public WireGuardEngineController get() {
    return newInstance(configBuilderProvider.get(), bridgeProvider.get());
  }

  public static WireGuardEngineController_Factory create(
      Provider<AmneziaWireGuardConfigBuilder> configBuilderProvider,
      Provider<WireGuardNativeBridge> bridgeProvider) {
    return new WireGuardEngineController_Factory(configBuilderProvider, bridgeProvider);
  }

  public static WireGuardEngineController newInstance(AmneziaWireGuardConfigBuilder configBuilder,
      WireGuardNativeBridge bridge) {
    return new WireGuardEngineController(configBuilder, bridge);
  }
}
