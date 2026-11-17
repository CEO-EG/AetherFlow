package com.ceoeg.aetherflow.core.common.security;

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
public final class EphemeralLoopbackAuthGenerator_Factory implements Factory<EphemeralLoopbackAuthGenerator> {
  @Override
  public EphemeralLoopbackAuthGenerator get() {
    return newInstance();
  }

  public static EphemeralLoopbackAuthGenerator_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static EphemeralLoopbackAuthGenerator newInstance() {
    return new EphemeralLoopbackAuthGenerator();
  }

  private static final class InstanceHolder {
    static final EphemeralLoopbackAuthGenerator_Factory INSTANCE = new EphemeralLoopbackAuthGenerator_Factory();
  }
}
