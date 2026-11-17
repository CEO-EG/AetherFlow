package com.ceoeg.aetherflow.core.routing;

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
public final class SplitTunnelAppManager_Factory implements Factory<SplitTunnelAppManager> {
  private final Provider<Context> contextProvider;

  private SplitTunnelAppManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public SplitTunnelAppManager get() {
    return newInstance(contextProvider.get());
  }

  public static SplitTunnelAppManager_Factory create(Provider<Context> contextProvider) {
    return new SplitTunnelAppManager_Factory(contextProvider);
  }

  public static SplitTunnelAppManager newInstance(Context context) {
    return new SplitTunnelAppManager(context);
  }
}
