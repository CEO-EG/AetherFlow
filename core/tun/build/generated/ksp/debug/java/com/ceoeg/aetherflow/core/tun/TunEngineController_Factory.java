package com.ceoeg.aetherflow.core.tun;

import android.content.Context;
import com.ceoeg.aetherflow.core.common.logging.DiagnosticLogger;
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
public final class TunEngineController_Factory implements Factory<TunEngineController> {
  private final Provider<Context> contextProvider;

  private final Provider<HevTunConfigBuilder> hevConfigBuilderProvider;

  private final Provider<HevTunNativeBridge> hevBridgeProvider;

  private final Provider<BadVpnNativeBridge> badVpnBridgeProvider;

  private final Provider<DiagnosticLogger> loggerProvider;

  private TunEngineController_Factory(Provider<Context> contextProvider,
      Provider<HevTunConfigBuilder> hevConfigBuilderProvider,
      Provider<HevTunNativeBridge> hevBridgeProvider,
      Provider<BadVpnNativeBridge> badVpnBridgeProvider,
      Provider<DiagnosticLogger> loggerProvider) {
    this.contextProvider = contextProvider;
    this.hevConfigBuilderProvider = hevConfigBuilderProvider;
    this.hevBridgeProvider = hevBridgeProvider;
    this.badVpnBridgeProvider = badVpnBridgeProvider;
    this.loggerProvider = loggerProvider;
  }

  @Override
  public TunEngineController get() {
    return newInstance(contextProvider.get(), hevConfigBuilderProvider.get(), hevBridgeProvider.get(), badVpnBridgeProvider.get(), loggerProvider.get());
  }

  public static TunEngineController_Factory create(Provider<Context> contextProvider,
      Provider<HevTunConfigBuilder> hevConfigBuilderProvider,
      Provider<HevTunNativeBridge> hevBridgeProvider,
      Provider<BadVpnNativeBridge> badVpnBridgeProvider,
      Provider<DiagnosticLogger> loggerProvider) {
    return new TunEngineController_Factory(contextProvider, hevConfigBuilderProvider, hevBridgeProvider, badVpnBridgeProvider, loggerProvider);
  }

  public static TunEngineController newInstance(Context context,
      HevTunConfigBuilder hevConfigBuilder, HevTunNativeBridge hevBridge,
      BadVpnNativeBridge badVpnBridge, DiagnosticLogger logger) {
    return new TunEngineController(context, hevConfigBuilder, hevBridge, badVpnBridge, logger);
  }
}
