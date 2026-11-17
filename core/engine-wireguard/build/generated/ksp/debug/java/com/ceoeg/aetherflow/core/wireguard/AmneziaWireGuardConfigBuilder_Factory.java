package com.ceoeg.aetherflow.core.wireguard;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class AmneziaWireGuardConfigBuilder_Factory implements Factory<AmneziaWireGuardConfigBuilder> {
  @Override
  public AmneziaWireGuardConfigBuilder get() {
    return newInstance();
  }

  public static AmneziaWireGuardConfigBuilder_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static AmneziaWireGuardConfigBuilder newInstance() {
    return new AmneziaWireGuardConfigBuilder();
  }

  private static final class InstanceHolder {
    static final AmneziaWireGuardConfigBuilder_Factory INSTANCE = new AmneziaWireGuardConfigBuilder_Factory();
  }
}
