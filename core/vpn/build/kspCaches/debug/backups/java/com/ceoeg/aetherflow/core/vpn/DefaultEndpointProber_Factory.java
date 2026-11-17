package com.ceoeg.aetherflow.core.vpn;

import com.ceoeg.aetherflow.core.xray.XrayConfigBuilder;
import com.ceoeg.aetherflow.core.xray.XrayNativeBridge;
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
public final class DefaultEndpointProber_Factory implements Factory<DefaultEndpointProber> {
  private final Provider<XrayConfigBuilder> xrayConfigBuilderProvider;

  private final Provider<XrayNativeBridge> xrayBridgeProvider;

  private DefaultEndpointProber_Factory(Provider<XrayConfigBuilder> xrayConfigBuilderProvider,
      Provider<XrayNativeBridge> xrayBridgeProvider) {
    this.xrayConfigBuilderProvider = xrayConfigBuilderProvider;
    this.xrayBridgeProvider = xrayBridgeProvider;
  }

  @Override
  public DefaultEndpointProber get() {
    return newInstance(xrayConfigBuilderProvider.get(), xrayBridgeProvider.get());
  }

  public static DefaultEndpointProber_Factory create(
      Provider<XrayConfigBuilder> xrayConfigBuilderProvider,
      Provider<XrayNativeBridge> xrayBridgeProvider) {
    return new DefaultEndpointProber_Factory(xrayConfigBuilderProvider, xrayBridgeProvider);
  }

  public static DefaultEndpointProber newInstance(XrayConfigBuilder xrayConfigBuilder,
      XrayNativeBridge xrayBridge) {
    return new DefaultEndpointProber(xrayConfigBuilder, xrayBridge);
  }
}
