package com.ceoeg.aetherflow.core.tun;

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
public final class HevTunConfigBuilder_Factory implements Factory<HevTunConfigBuilder> {
  @Override
  public HevTunConfigBuilder get() {
    return newInstance();
  }

  public static HevTunConfigBuilder_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static HevTunConfigBuilder newInstance() {
    return new HevTunConfigBuilder();
  }

  private static final class InstanceHolder {
    static final HevTunConfigBuilder_Factory INSTANCE = new HevTunConfigBuilder_Factory();
  }
}
