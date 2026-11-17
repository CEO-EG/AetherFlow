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
public final class ExitNetworkVerifier_Factory implements Factory<ExitNetworkVerifier> {
  @Override
  public ExitNetworkVerifier get() {
    return newInstance();
  }

  public static ExitNetworkVerifier_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ExitNetworkVerifier newInstance() {
    return new ExitNetworkVerifier();
  }

  private static final class InstanceHolder {
    static final ExitNetworkVerifier_Factory INSTANCE = new ExitNetworkVerifier_Factory();
  }
}
