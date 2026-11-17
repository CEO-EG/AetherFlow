package com.ceoeg.aetherflow.core.singbox;

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
public final class SingBoxEngineController_Factory implements Factory<SingBoxEngineController> {
  private final Provider<SingBoxNativeBridge> bridgeProvider;

  private SingBoxEngineController_Factory(Provider<SingBoxNativeBridge> bridgeProvider) {
    this.bridgeProvider = bridgeProvider;
  }

  @Override
  public SingBoxEngineController get() {
    return newInstance(bridgeProvider.get());
  }

  public static SingBoxEngineController_Factory create(
      Provider<SingBoxNativeBridge> bridgeProvider) {
    return new SingBoxEngineController_Factory(bridgeProvider);
  }

  public static SingBoxEngineController newInstance(SingBoxNativeBridge bridge) {
    return new SingBoxEngineController(bridge);
  }
}
