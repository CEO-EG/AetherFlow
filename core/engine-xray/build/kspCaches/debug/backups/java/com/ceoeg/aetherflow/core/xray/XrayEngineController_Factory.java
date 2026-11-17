package com.ceoeg.aetherflow.core.xray;

import android.content.Context;
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
public final class XrayEngineController_Factory implements Factory<XrayEngineController> {
  private final Provider<Context> contextProvider;

  private final Provider<XrayNativeBridge> bridgeProvider;

  private XrayEngineController_Factory(Provider<Context> contextProvider,
      Provider<XrayNativeBridge> bridgeProvider) {
    this.contextProvider = contextProvider;
    this.bridgeProvider = bridgeProvider;
  }

  @Override
  public XrayEngineController get() {
    return newInstance(contextProvider.get(), bridgeProvider.get());
  }

  public static XrayEngineController_Factory create(Provider<Context> contextProvider,
      Provider<XrayNativeBridge> bridgeProvider) {
    return new XrayEngineController_Factory(contextProvider, bridgeProvider);
  }

  public static XrayEngineController newInstance(Context context, XrayNativeBridge bridge) {
    return new XrayEngineController(context, bridge);
  }
}
