package com.ceoeg.aetherflow.core.vpn;

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
public final class VpnStateMachine_Factory implements Factory<VpnStateMachine> {
  @Override
  public VpnStateMachine get() {
    return newInstance();
  }

  public static VpnStateMachine_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static VpnStateMachine newInstance() {
    return new VpnStateMachine();
  }

  private static final class InstanceHolder {
    static final VpnStateMachine_Factory INSTANCE = new VpnStateMachine_Factory();
  }
}
