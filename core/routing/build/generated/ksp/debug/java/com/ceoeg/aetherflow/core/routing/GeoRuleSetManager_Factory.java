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
public final class GeoRuleSetManager_Factory implements Factory<GeoRuleSetManager> {
  private final Provider<Context> contextProvider;

  private GeoRuleSetManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public GeoRuleSetManager get() {
    return newInstance(contextProvider.get());
  }

  public static GeoRuleSetManager_Factory create(Provider<Context> contextProvider) {
    return new GeoRuleSetManager_Factory(contextProvider);
  }

  public static GeoRuleSetManager newInstance(Context context) {
    return new GeoRuleSetManager(context);
  }
}
