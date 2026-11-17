package com.ceoeg.aetherflow.core.vpn;

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
public final class SmartNodeSelector_Factory implements Factory<SmartNodeSelector> {
  private final Provider<EndpointProber> proberProvider;

  private SmartNodeSelector_Factory(Provider<EndpointProber> proberProvider) {
    this.proberProvider = proberProvider;
  }

  @Override
  public SmartNodeSelector get() {
    return newInstance(proberProvider.get());
  }

  public static SmartNodeSelector_Factory create(Provider<EndpointProber> proberProvider) {
    return new SmartNodeSelector_Factory(proberProvider);
  }

  public static SmartNodeSelector newInstance(EndpointProber prober) {
    return new SmartNodeSelector(prober);
  }
}
