package com.ceoeg.aetherflow.core.singbox;

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
public final class ClashApiInspectorClient_Factory implements Factory<ClashApiInspectorClient> {
  @Override
  public ClashApiInspectorClient get() {
    return newInstance();
  }

  public static ClashApiInspectorClient_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ClashApiInspectorClient newInstance() {
    return new ClashApiInspectorClient();
  }

  private static final class InstanceHolder {
    static final ClashApiInspectorClient_Factory INSTANCE = new ClashApiInspectorClient_Factory();
  }
}
